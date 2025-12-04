package com.hao.datacollector.report.mysql;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.RowMapper;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.TimeUnit;

@SpringBootTest
public class QuotationBenchmarkTest {

    @Autowired
    private JdbcTemplate jdbcTemplate;

    // 测试参数
    private final String STOCK_CODE = "600519.SH"; // 贵州茅台
    private final String START_DATE = "2024-01-20";
    private final String END_DATE = "2024-03-05";
    
    // 预热次数和测试次数
    private final int WARMUP_CYCLES = 5;
    private final int TEST_CYCLES = 10;

    @Test
    public void benchmarkCrossMonthQuery() {
        System.out.println("=======================================================");
        System.out.println("🔥 开始性能对比压测：跨月范围查询 (Range Select)");
        System.out.println("📅 时间范围: " + START_DATE + " 至 " + END_DATE);
        System.out.println("🎯 目标股票: " + STOCK_CODE);
        System.out.println("=======================================================\n");

        // 1. 构造 SQL
        String oldTableSql = generateOldTableUnionSql();
        String newTableSql = generateNewTableSql();

        // 2. 验证 SQL 逻辑并获取数据行数 (确保数据一致)
        int oldCount = verifyAndCount(oldTableSql, "老表模式");
        int newCount = verifyAndCount(newTableSql, "新表模式");

        if (oldCount != newCount) {
            System.err.println("❌ 警告：新老表查询结果行数不一致！请检查数据迁移完整性。");
        } else {
            System.out.println("✅ 数据一致性校验通过，行数: " + newCount + "\n");
        }

        // 3. 运行压测
        long oldTableAvgTime = runBenchmark("老表模式 (UNION ALL)", oldTableSql);
        long newTableAvgTime = runBenchmark("新表模式 (分区表)", newTableSql);

        // 4. 打印结论
        printConclusion(oldTableAvgTime, newTableAvgTime);
    }

    /**
     * 生成新表 SQL (极其简洁)
     */
    private String generateNewTableSql() {
        return String.format(
            "SELECT * FROM tb_quotation_history_hot " +
            "WHERE wind_code = '%s' " +
            "AND trade_date BETWEEN '%s' AND '%s'",
            STOCK_CODE, START_DATE, END_DATE
        );
    }

    /**
     * 生成老表 SQL (模拟应用层的 UNION ALL 拼接噩梦)
     * 这里的逻辑是模拟 Java 代码动态计算月份并拼接 SQL
     */
    private String generateOldTableUnionSql() {
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
     */
    private long runBenchmark(String scenarioName, String sql) {
        System.out.println("🚀 开始压测场景: " + scenarioName);
        
        // 预热 (Warmup) - 让数据库加载索引页到内存，消除冷启动差异
        System.out.print("   正在预热...");
        for (int i = 0; i < WARMUP_CYCLES; i++) {
            jdbcTemplate.query(sql, (rs) -> {});
        }
        System.out.println("完成");

        // 正式测试
        List<Long> costs = new ArrayList<>();
        System.out.print("   正在执行 " + TEST_CYCLES + " 次查询...");
        
        for (int i = 0; i < TEST_CYCLES; i++) {
            long start = System.nanoTime();
            jdbcTemplate.query(sql, (rs) -> {}); // 执行查询并遍历结果集
            long end = System.nanoTime();
            costs.add(TimeUnit.NANOSECONDS.toMillis(end - start));
            System.out.print(".");
        }
        System.out.println();

        // 计算平均耗时
        double avgTime = costs.stream().mapToLong(Long::longValue).average().orElse(0.0);
        System.out.printf("   ⏱️ 平均耗时: %.2f ms%n", avgTime);
        System.out.println("-------------------------------------------------------");
        
        return (long) avgTime;
    }

    private int verifyAndCount(String sql, String name) {
        try {
            List<Integer> rows = jdbcTemplate.query(sql, new RowMapper<Integer>() {
                @Override
                public Integer mapRow(ResultSet rs, int rowNum) throws SQLException {
                    return 1;
                }
            });
            return rows.size();
        } catch (Exception e) {
            System.err.println("❌ " + name + " SQL 执行失败: " + e.getMessage());
            return -1;
        }
    }
    
    private void printConclusion(long oldTime, long newTime) {
        System.out.println("\n🏆 === 最终对比结论 ===");
        System.out.println("老表架构耗时: " + oldTime + " ms");
        System.out.println("新表架构耗时: " + newTime + " ms");
        
        if (newTime < oldTime) {
            double improvement = ((double)(oldTime - newTime) / oldTime) * 100;
            System.out.printf("🚀 性能提升: %.2f%%%n", improvement);
            System.out.println("🌟 评价: 分区表架构不仅简化了代码，还带来了显著的性能优势！");
        } else if (Math.abs(newTime - oldTime) < 5) {
            System.out.println("⚖️ 评价: 性能持平。考虑到新表极大地降低了代码维护成本（无需分表逻辑），这依然是一次巨大的胜利！");
        } else {
            System.out.println("🤔 评价: 新表略慢。请检查 EXPLAIN 计划是否正确触发了分区裁剪 (Partition Pruning)。");
        }
        System.out.println("=======================================================");
    }
}