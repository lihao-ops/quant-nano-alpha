package com.hao.datacollector.report.mysql;

import org.junit.jupiter.api.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.RowMapper;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.TimeUnit;

/**
 * 行情历史表结构压测对比测试
 *
 * 测试目的：
 * 1. 对比老表UNION与新表分区查询性能差异。
 * 2. 校验新老表查询结果的一致性。
 *
 * 设计思路：
 * - 先进行预热再执行多轮查询，降低缓存抖动。
 */
@SpringBootTest
public class QuotationBenchmarkTest {
    private static final Logger LOG = LoggerFactory.getLogger(QuotationBenchmarkTest.class);

    @Autowired
    private JdbcTemplate jdbcTemplate;

    // 测试参数
    private final String STOCK_CODE = "600519.SH"; // 贵州茅台
    private final String START_DATE = "2024-01-20";
    private final String END_DATE = "2024-03-05";
    
    // 预热次数和测试次数
    private final int WARMUP_CYCLES = 5;
    private final int TEST_CYCLES = 10;

    /**
     * 跨月范围查询压测
     *
     * 实现逻辑：
     * 1. 生成新老表SQL并校验结果一致性。
     * 2. 执行多轮压测并计算平均耗时。
     * 3. 输出最终性能对比结论。
     */
    @Test
    public void benchmarkCrossMonthQuery() {
        // 实现思路：统一生成SQL并对比新老表性能与结果一致性
        LOG.info("压测开始|Benchmark_start");
        LOG.info("压测范围|Benchmark_range,startDate={},endDate={}", START_DATE, END_DATE);
        LOG.info("目标股票|Target_stock,stockCode={}", STOCK_CODE);

        // 1. 构造 SQL
        String oldTableSql = generateOldTableUnionSql();
        String newTableSql = generateNewTableSql();

        // 2. 验证 SQL 逻辑并获取数据行数 (确保数据一致)
        int oldCount = verifyAndCount(oldTableSql, "老表模式");
        int newCount = verifyAndCount(newTableSql, "新表模式");

        if (oldCount != newCount) {
            LOG.warn("数据一致性校验失败|Data_consistency_failed,oldCount={},newCount={}", oldCount, newCount);
        } else {
            LOG.info("数据一致性校验通过|Data_consistency_pass,count={}", newCount);
        }

        // 3. 运行压测
        long oldTableAvgTime = runBenchmark("老表模式 (UNION ALL)", oldTableSql);
        long newTableAvgTime = runBenchmark("新表模式 (分区表)", newTableSql);

        // 4. 打印结论
        printConclusion(oldTableAvgTime, newTableAvgTime);
    }

    /**
     * 生成新表SQL
     *
     * 实现逻辑：
     * 1. 使用分区主表与时间范围过滤。
     * 2. 保持SQL简洁以体现结构优势。
     */
    private String generateNewTableSql() {
        // 实现思路：使用单表查询并走分区裁剪
        return String.format(
            "SELECT * FROM tb_quotation_history_hot " +
            "WHERE wind_code = '%s' " +
            "AND trade_date BETWEEN '%s' AND '%s'",
            STOCK_CODE, START_DATE, END_DATE
        );
    }

    /**
     * 生成老表SQL
     *
     * 实现逻辑：
     * 1. 根据月份拆分表名。
     * 2. 使用UNION ALL拼接成完整查询。
     */
    private String generateOldTableUnionSql() {
        // 实现思路：模拟业务层动态拼接分表SQL
        // 模拟业务逻辑：计算出涉及 202401, 202402, 202403 三张表
        String[] tables = {
            "tb_quotation_history_trend_202401",
            "tb_quotation_history_trend_202402",
            "tb_quotation_history_trend_202403"
        };

        StringBuilder sqlBuilder = new StringBuilder();
        for (int i = 0; i < tables.length; i++) {
            if (i > 0) {
                sqlBuilder.append(" UNION ALL ");
            }
            // 注意：为了公平对比，老表查询也加上时间范围过滤，利用索引
            sqlBuilder.append(String.format(
                "SELECT * FROM %s WHERE wind_code = '%s' AND trade_date BETWEEN '%s' AND '%s'",
                tables[i], STOCK_CODE, START_DATE, END_DATE
            ));
        }
        return sqlBuilder.toString();
    }

    /**
     * 执行压测核心逻辑
     *
     * 实现逻辑：
     * 1. 先进行预热以消除冷启动抖动。
     * 2. 执行多轮查询并统计耗时。
     * 3. 计算平均耗时作为结果。
     *
     * @param scenarioName 场景名称
     * @param sql          执行SQL
     * @return 平均耗时
     */
    private long runBenchmark(String scenarioName, String sql) {
        // 实现思路：预热后多轮执行计算平均耗时
        LOG.info("压测场景开始|Benchmark_scenario_start,scenario={}", scenarioName);
        // 预热 (Warmup) - 让数据库加载索引页到内存，消除冷启动差异
        LOG.info("压测预热开始|Benchmark_warmup_start,cycle={}", WARMUP_CYCLES);
        for (int i = 0; i < WARMUP_CYCLES; i++) {
            jdbcTemplate.query(sql, (rs) -> {});
        }
        LOG.info("压测预热完成|Benchmark_warmup_done");

        // 正式测试
        List<Long> costs = new ArrayList<>();
        LOG.info("压测执行开始|Benchmark_execution_start,cycle={}", TEST_CYCLES);
        for (int i = 0; i < TEST_CYCLES; i++) {
            long start = System.nanoTime();
            jdbcTemplate.query(sql, (rs) -> {}); // 执行查询并遍历结果集
            long end = System.nanoTime();
            costs.add(TimeUnit.NANOSECONDS.toMillis(end - start));
        }
        LOG.info("压测执行完成|Benchmark_execution_done");

        // 计算平均耗时
        double avgTime = costs.stream().mapToLong(Long::longValue).average().orElse(0.0);
        LOG.info("压测平均耗时|Benchmark_avg_cost_ms,avgMs={}", avgTime);
        return (long) avgTime;
    }

    /**
     * 执行SQL并统计行数
     *
     * 实现逻辑：
     * 1. 执行查询并遍历结果。
     * 2. 返回行数用于一致性校验。
     *
     * @param sql  SQL语句
     * @param name 场景名称
     * @return 行数
     */
    private int verifyAndCount(String sql, String name) {
        // 实现思路：遍历结果集计数并在异常时记录日志
        try {
            List<Integer> rows = jdbcTemplate.query(sql, new RowMapper<Integer>() {
                @Override
                public Integer mapRow(ResultSet rs, int rowNum) throws SQLException {
                    return 1;
                }
            });
            return rows.size();
        } catch (Exception e) {
            LOG.error("SQL执行失败|Sql_execute_failed,scenario={},error={}", name, e.getMessage(), e);
            return -1;
        }
    }
    
    /**
     * 输出压测结论
     *
     * 实现逻辑：
     * 1. 输出新老表平均耗时。
     * 2. 根据差异输出结论评价。
     *
     * @param oldTime 老表平均耗时
     * @param newTime 新表平均耗时
     */
    private void printConclusion(long oldTime, long newTime) {
        // 实现思路：根据耗时差异输出结论信息
        LOG.info("压测结论输出|Benchmark_conclusion_output,oldMs={},newMs={}", oldTime, newTime);
        if (newTime < oldTime) {
            double improvement = ((double)(oldTime - newTime) / oldTime) * 100;
            LOG.info("性能提升|Performance_improvement,percent={}", improvement);
            LOG.info("结论评价|Conclusion_comment,comment=分区表架构带来性能优势");
        } else if (Math.abs(newTime - oldTime) < 5) {
            LOG.info("结论评价|Conclusion_comment,comment=性能持平但维护成本下降");
        } else {
            LOG.warn("结论评价|Conclusion_comment,comment=新表略慢需检查分区裁剪");
        }
    }
}
