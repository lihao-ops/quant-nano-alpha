package com.hao.datacollector.report.mysql;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.hao.datacollector.cache.StockCache;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.namedparam.MapSqlParameterSource;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.transaction.support.TransactionTemplate;
import org.springframework.util.StopWatch;

import java.util.List;

/**
 * 核心压测类：数据库覆盖索引 (Covering Index) vs 普通索引回表 (Table Lookup) 性能对比
 * <p>
 * 1. 【测试目的】
 * 验证在海量数据（2000万+行）场景下，通过"覆盖索引"优化，消除"回表"操作带来的随机磁盘 I/O，
 * 从而量化评估其对查询性能的提升幅度。
 * <p>
 * 2. 【测试场景】
 * 模拟真实的股票行情查询：查询特定时间段内（一个月），一批热门股票（约500只）的量价数据。
 * - 数据量级：单表 2200万行。
 * - 查询比例：约占总数据量的 10%（这是优化器最容易“纠结”的区间，最能体现索引价值）。
 * <p>
 * 3. 【预期结果 (Hypothesis)】
 * - Round 1 (普通索引):
 * MySQL 走二级索引 `uniq_windcode_tradedate` 定位 ID，但必须回表读取 `latest_price` 等字段。
 * 预期现象：Optimizer Trace 显示 index_only=false，物理 I/O 高（产生大量随机读取），耗时较长。
 * - Round 2 (覆盖索引):
 * MySQL 走二级索引 `idx_covering_perf`，所需字段全在索引树上。
 * 预期现象：Optimizer Trace 显示 index_only=true，物理 I/O 极低（甚至为0），耗时极短。
 * <p>
 * 4. 【评估标准】
 * - 核心指标：物理 I/O (Innodb_data_reads)。这是比时间更“硬”的指标，不受 CPU 波动影响。
 * - 辅助指标：执行耗时 (ms)。
 * - 验证手段：Optimizer Trace 中的 `index_only: true/false` 标记。
 * <p>
 * 5. 【技术亮点】
 * - 流式查询 (Stream): 使用 RowCallbackHandler 防止将 20万行数据加载进内存导致 OOM。
 * - 深度追踪 (Trace): 集成 MySQL Optimizer Trace 捕获优化器决策成本 (Cost)。
 * - 动态采样: 自动计算 10% 数据量，确保测试的科学性。
 */
@SpringBootTest
public class IndexHeavyTest {

    private static final Logger logger = LoggerFactory.getLogger(IndexHeavyTest.class);

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @Autowired
    private NamedParameterJdbcTemplate namedJdbcTemplate;

    @Autowired
    private TransactionTemplate transactionTemplate;

    @Autowired
    private ObjectMapper objectMapper;

    // 测试时间范围
    private final String START_DATE = "2025-01-01 00:00:00";
    private final String END_DATE = "2025-02-01 23:59:59";

    // 动态采样比例 (10%)
    private final double TEST_DATA_RATIO = 0.10;

    @Test
    @DisplayName("深度压测：覆盖索引 vs 普通索引 (含完整 Trace)")
    public void runHeavyBenchmark() {
        logger.info("日志记录|Log_message,========================================================================");
        logger.info("_开始深度压测_(Index_Range_Scan_Simulation)");
        logger.info("__包含完整_Optimizer_Trace，控制台日志会很长");
        logger.info("日志记录|Log_message,========================================================================");

        // Step 1: 准备数据
        List<String> allCodes = StockCache.allWindCode;
        if (allCodes == null || allCodes.isEmpty()) throw new RuntimeException(" 无数据");

        // [Step 1] 动态数据准备
        // 目的：制造一个"既不能全表扫描，又不能只读几行"的尴尬区间(10%-15%)。
        // 在这个区间下，如果索引设计不好，MySQL会被迫进行大量的随机磁盘 I/O。
        int limitSize = Math.max(1, (int) (allCodes.size() * TEST_DATA_RATIO));
        List<String> targetCodes = allCodes.subList(0, limitSize);

        logger.info("日志记录|Log_message,_样本总数:_{}_(比例:_{}%)", targetCodes.size(), (int) (TEST_DATA_RATIO * 100));

        MapSqlParameterSource params = new MapSqlParameterSource();
        params.addValue("codes", targetCodes);
        params.addValue("startDate", START_DATE);
        params.addValue("endDate", END_DATE);

        // Step 2: 准备 SQL
        String sqlBase = """
                SELECT wind_code, trade_date, latest_price, total_volume, average_price 
                FROM tb_hot_test_base 
                WHERE wind_code IN (:codes) 
                AND trade_date BETWEEN :startDate AND :endDate
                """;

        String sqlCover = """
                SELECT wind_code, trade_date, latest_price, total_volume, average_price 
                FROM tb_hot_test_cover 
                WHERE wind_code IN (:codes) 
                AND trade_date BETWEEN :startDate AND :endDate
                """;

        StopWatch stopWatch = new StopWatch("索引性能对比");

        // =======================================================================
        // [Round 1] 测试普通索引表 (tb_hot_test_base)
        // 预期行为：二级索引 -> 获取主键ID -> 回主键索引查数据页 (回表)
        // =======================================================================

        // 1.1 [验证阶段] 开启 Trace，询问 MySQL 优化器："你打算怎么查？"
        // 这一步只做 Explain，不产生真实数据 I/O，为了拿到 cost 和 index_only 状态。
        logger.info("\n>>>_[Round_1]_分析:_tb_hot_test_base_(普通索引)");

        // 1.1 深度追踪
        runOptimizerTrace(sqlBase, params, "tb_hot_test_base");

        // 1.2 物理 I/O 记录
        long startReadsBase = getPhysicalReads();

        // 1.3 压测
        stopWatch.start("普通索引(回表)");
        Integer rowCountBase = namedJdbcTemplate.query(sqlBase, params, rs -> {
            int count = 0;
            while (rs.next()) {
                rs.getObject("wind_code");
                count++;
            }
            return count;
        });
        stopWatch.stop();

        long ioCostBase = getPhysicalReads() - startReadsBase;
        logger.info("[Round_1_结果]_耗时:_{}_ms_|_I/O:_{}_|_行数:_{}", stopWatch.getLastTaskTimeMillis(), ioCostBase, rowCountBase);

        // =======================================================================
        // [Round 2] 测试覆盖索引表 (tb_hot_test_cover)
        // 预期行为：二级索引直接提供所有数据 (零回表)
        // =======================================================================
        logger.info("\n>>>_[Round_2]_分析:_tb_hot_test_cover_(覆盖索引)");

        // 2.1 深度追踪
        runOptimizerTrace(sqlCover, params, "tb_hot_test_cover");

        // 2.2 物理 I/O 记录
        long startReadsCover = getPhysicalReads();

        // 2.3 压测
        stopWatch.start("覆盖索引(无回表)");
        Integer rowCountCover = namedJdbcTemplate.query(sqlCover, params, rs -> {
            int count = 0;
            while (rs.next()) {
                rs.getObject("wind_code");
                count++;
            }
            return count;
        });
        stopWatch.stop();

        long ioCostCover = getPhysicalReads() - startReadsCover;
        logger.info("[Round_2_结果]_耗时:_{}_ms_|_I/O:_{}_|_行数:_{}", stopWatch.getLastTaskTimeMillis(), ioCostCover, rowCountCover);

        printReport(stopWatch, ioCostBase, ioCostCover, rowCountBase);
    }

    private void runOptimizerTrace(String sql, MapSqlParameterSource params, String tableName) {
        logger.info("---_正在生成_Optimizer_Trace_JSON_(请查看下方日志)_---");
        transactionTemplate.execute(status -> {
            try {
                // 1. 开启 Trace
                jdbcTemplate.execute("SET SESSION optimizer_trace='enabled=on'");

                // 2. 执行 EXPLAIN
                namedJdbcTemplate.queryForList("EXPLAIN " + sql, params);

                // 3. 提取 Trace
                List<String> traces = jdbcTemplate.query(
                        "SELECT TRACE FROM information_schema.OPTIMIZER_TRACE",
                        (rs, rowNum) -> rs.getString("TRACE")
                );

                // 4. 获取 Cost
                String cost = jdbcTemplate.queryForObject(
                        "SHOW STATUS LIKE 'Last_query_cost'",
                        (rs, rowNum) -> rs.getString("Value")
                );

                // 5. 打印完整 JSON
                if (!traces.isEmpty()) {
                    String rawJson = traces.get(0);
                    // 核心修改：使用 Jackson 格式化 JSON 并完整打印
                    try {
                        Object jsonObject = objectMapper.readValue(rawJson, Object.class);
                        String prettyJson = objectMapper.writerWithDefaultPrettyPrinter().writeValueAsString(jsonObject);

                        logger.info("_[Trace_成本]_Cost:_{}", cost);
                        logger.info("_[Trace_详情]_{}:\n{}", tableName, prettyJson);

                    } catch (Exception e) {
                        logger.warn("JSON_格式化失败，打印原始字符串:_{}", rawJson);
                    }

                    if (tableName.contains("cover") && (rawJson.contains("covering_index") || rawJson.contains("Using index"))) {
                        logger.info("日志记录|Log_message,_校验通过:_优化器确认使用了覆盖索引");
                    }
                }

                // 6. 关闭
                jdbcTemplate.execute("SET SESSION optimizer_trace='enabled=off'");
            } catch (Exception e) {
                logger.error("_Trace_失败", e);
            }
            return null;
        });
    }

    private long getPhysicalReads() {
        try {
            return jdbcTemplate.queryForObject(
                    "SHOW STATUS LIKE 'Innodb_data_reads'",
                    (rs, rowNum) -> rs.getLong("Value")
            );
        } catch (Exception e) {
            return 0;
        }
    }

    private void logSqlExecution(String sql, String start, String end, int count) {
        logger.info("SQL预览:_[Code数:_{},_日期:_{}_-_{}]", count, start, end);
    }

    private void printReport(StopWatch stopWatch, long ioBase, long ioCover, int rows) {
        long t1 = stopWatch.getTaskInfo()[0].getTimeMillis();
        long t2 = stopWatch.getTaskInfo()[1].getTimeMillis();
        logger.info("\n====================_最终报告_====================");
        logger.info("普通索引:_{}_ms_(I/O:_{})", t1, ioBase);
        logger.info("覆盖索引:_{}_ms_(I/O:_{})", t2, ioCover);
        if (t1 > t2) logger.info("日志记录|Log_message,_覆盖索引快了_{}%", String.format("%.2f", (double) (t1 - t2) / t1 * 100));
        logger.info("日志记录|Log_message,==================================================");
    }
}