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
import java.util.Map;

/**
 * 最终压测版 (Final Ultimate Edition v2)
 * 修复记录：
 * 1. 修复 SQL 语法空格问题
 * 2. 修复 SHOW STATUS 返回多列导致的映射异常
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
    private final String END_DATE = "2025-01-03 23:59:59";

    // 动态采样比例 (10%)
    private final double TEST_DATA_RATIO = 0.10;

    @Test
    @DisplayName("深度压测：覆盖索引 vs 普通索引 (含 Trace 追踪)")
    public void runHeavyBenchmark() {
        logger.info("========================================================================");
        logger.info("  🚀 开始深度压测 (Index Range Scan Simulation)");
        logger.info("  ⚠️ 包含 Optimizer Trace 追踪");
        logger.info("========================================================================");

        // Step 1: 准备数据
        List<String> allCodes = StockCache.allWindCode;
        if (allCodes == null || allCodes.isEmpty()) throw new RuntimeException("❌ 无数据");

        int limitSize = Math.max(1, (int) (allCodes.size() * TEST_DATA_RATIO));
        List<String> targetCodes = allCodes.subList(0, limitSize);

        logger.info("✅ 样本总数: {} (比例: {}%)", targetCodes.size(), (int) (TEST_DATA_RATIO * 100));

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
        // Round 1: 普通索引
        // =======================================================================
        logger.info("\n>>> [Round 1] 分析: tb_hot_test_base (预期：Cost高/回表)");

        // 1.1 深度追踪
        runOptimizerTrace(sqlBase, params, "tb_hot_test_base");

        // 1.2 物理 I/O 记录
        long startReadsBase = getPhysicalReads();

        // 1.3 压测
        stopWatch.start("普通索引(回表)");
        Integer rowCountBase = namedJdbcTemplate.query(sqlBase, params, rs -> {
            int count = 0;
            while (rs.next()) { rs.getObject("wind_code"); count++; }
            return count;
        });
        stopWatch.stop();

        long ioCostBase = getPhysicalReads() - startReadsBase;
        logger.info("[Round 1 结果] 耗时: {} ms | I/O: {} | 行数: {}", stopWatch.getLastTaskTimeMillis(), ioCostBase, rowCountBase);

        // =======================================================================
        // Round 2: 覆盖索引
        // =======================================================================
        logger.info("\n>>> [Round 2] 分析: tb_hot_test_cover (预期：Cost低/覆盖索引)");

        // 2.1 深度追踪
        runOptimizerTrace(sqlCover, params, "tb_hot_test_cover");

        // 2.2 物理 I/O 记录
        long startReadsCover = getPhysicalReads();

        // 2.3 压测
        stopWatch.start("覆盖索引(无回表)");
        Integer rowCountCover = namedJdbcTemplate.query(sqlCover, params, rs -> {
            int count = 0;
            while (rs.next()) { rs.getObject("wind_code"); count++; }
            return count;
        });
        stopWatch.stop();

        long ioCostCover = getPhysicalReads() - startReadsCover;
        logger.info("[Round 2 结果] 耗时: {} ms | I/O: {} | 行数: {}", stopWatch.getLastTaskTimeMillis(), ioCostCover, rowCountCover);

        printReport(stopWatch, ioCostBase, ioCostCover, rowCountBase);
    }

    private void runOptimizerTrace(String sql, MapSqlParameterSource params, String tableName) {
        logger.info("--- 正在执行 Optimizer Trace ---");
        transactionTemplate.execute(status -> {
            try {
                // 1. 开启 Trace (仅 enabled)
                jdbcTemplate.execute("SET SESSION optimizer_trace='enabled=on'");

                // 2. 执行 EXPLAIN
                namedJdbcTemplate.queryForList("EXPLAIN " + sql, params);

                // 3. 提取 Trace
                List<String> traces = jdbcTemplate.query(
                        "SELECT TRACE FROM information_schema.OPTIMIZER_TRACE",
                        (rs, rowNum) -> rs.getString("TRACE")
                );

                // 4. 获取 Cost (✅ 修复：显式映射 Value 列)
                String cost = jdbcTemplate.queryForObject(
                        "SHOW STATUS LIKE 'Last_query_cost'",
                        (rs, rowNum) -> rs.getString("Value")
                );

                // 5. 打印
                if (!traces.isEmpty()) {
                    String rawJson = traces.get(0);
                    // 仅打印前 1000 字符避免刷屏，或者完整打印
                    // String prettyJson = objectMapper.writerWithDefaultPrettyPrinter().writeValueAsString(objectMapper.readValue(rawJson, Object.class));
                    // logger.info("🔍 Trace:\n{}", prettyJson);

                    logger.info("📊 [Trace结果] 表: {} | 优化器预估成本(Cost): {}", tableName, cost);

                    if (tableName.contains("cover") && (rawJson.contains("covering_index") || rawJson.contains("Using index"))) {
                        logger.info("✅ 校验通过: 优化器确认使用了覆盖索引");
                    }
                }

                // 6. 关闭
                jdbcTemplate.execute("SET SESSION optimizer_trace='enabled=off'");
            } catch (Exception e) {
                logger.error("❌ Trace 失败", e);
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
        } catch (Exception e) { return 0; }
    }

    private void printReport(StopWatch stopWatch, long ioBase, long ioCover, int rows) {
        long t1 = stopWatch.getTaskInfo()[0].getTimeMillis();
        long t2 = stopWatch.getTaskInfo()[1].getTimeMillis();
        logger.info("\n==================== 最终报告 ====================");
        logger.info("普通索引: {} ms (I/O: {})", t1, ioBase);
        logger.info("覆盖索引: {} ms (I/O: {})", t2, ioCover);
        if (t1 > t2) logger.info("🚀 覆盖索引快了 {}%", String.format("%.2f", (double) (t1 - t2) / t1 * 100));
        logger.info("==================================================");
    }
}