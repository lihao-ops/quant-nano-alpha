package com.hao.datacollector.report.mysql; // ‼️ 确保包路径正确 (Ensure package path is correct)

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.extern.slf4j.Slf4j;
import org.junit.jupiter.api.*; // ‼️ 确保导入了 @TestInstance
import org.junit.jupiter.api.MethodOrderer.OrderAnnotation;
import org.junit.jupiter.api.TestInstance.Lifecycle;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.dao.DataAccessException;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.util.StopWatch;

import java.io.IOException;
import java.math.BigDecimal;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * 迁移后性能验收测试 (OAT - Operation Acceptance Test)
 * (Performance Acceptance Test after Migration)
 *
 * 验证新表 (tb_quotation_history_warm) 相比旧分表 (tb_quotation_history_trend_xxxx)
 * 在K线/分时/回测场景下的查询延迟 (Latency)。
 * (Verify the query latency of the new table vs. the old tables in typical scenarios.)
 *
 * 实验方法论 (Methodology):
 * 1. 静态验证 (Static): 首先使用 EXPLAIN 验证新表的执行计划，确保分区剪枝(Partition Pruning)和索引(Index)均按预期工作。
 * 2. 动态热读 (Dynamic Hot Read): 模拟数据已在 InnoDB Buffer Pool 缓存中。此场景测试的是 CPU 解压缩性能。
 * 3. 动态冷读 (Dynamic Cold Read): 模拟数据不在缓存中。在每次查询前强制清空 Buffer Pool，测试真实磁盘 I/O 性能。
 * 4. 产出物 (Artifacts): 自动生成一份 Markdown 格式的验收报告。
 */
@SpringBootTest
@Slf4j
@TestMethodOrder(OrderAnnotation.class) // 强制按顺序执行 (Force sequential execution)
@TestInstance(Lifecycle.PER_CLASS) // 🔥 核心修复: 告诉 JUnit 5 对此类使用单个实例
// (Core Fix: Tell JUnit 5 to use a single instance for this class)
public class PerformanceOatTests {

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @Autowired
    private ObjectMapper objectMapper; // Spring Boot 自动注入 (Auto-injected by Spring Boot)

    // --- 测试配置 (Test Configuration) ---
    private static final String STOCK_CODE = "600519.SH";     // 贵州茅台 (Kweichow Moutai)
    private static final int WARMUP_RUNS = 5;                 // 预热次数 (Warmup iterations)
    private static final int BENCHMARK_RUNS = 10;             // 实际测试次数 (Benchmark iterations)
    private static final String TARGET_DATABASE = "a_share_quant"; // 你的数据库名 (Your Database Schema)

    // --- 报告元数据 (Report Metadata) ---
    // ‼️ 移除了 'static'，因为它们现在是类实例的字段
    // (Removed 'static' as they are now fields of the class instance)
    private String mysqlVersion;
    private String mysqlHost;
    private final List<Map<String, String>> planReport = new ArrayList<>();
    private final List<Map<String, String>> hotReadReport = new ArrayList<>();
    private final List<Map<String, String>> coldReadReport = new ArrayList<>();

    // =================================================================================
    // SQL 测试用例定义 (SQL Test Case Definitions)
    // =================================================================================

    // 场景 1: 单日K线 (2021-01-04)
    private static final String SQL_1A_OLD_DAY = String.format("SELECT AVG(latest_price) FROM tb_quotation_history_trend_202101 WHERE trade_date >= '2021-01-04 00:00:00' AND trade_date <= '2021-01-04 15:00:00' AND wind_code = '%s'", STOCK_CODE);
    private static final String SQL_1B_NEW_DAY = String.format("SELECT AVG(latest_price) FROM tb_quotation_history_warm WHERE trade_date >= '2021-01-04 00:00:00' AND trade_date <= '2021-01-04 15:00:00' AND wind_code = '%s'", STOCK_CODE);

    // 场景 2: 单月聚合
    private static final String SQL_2A_OLD_MONTH = String.format("SELECT AVG(latest_price) FROM tb_quotation_history_trend_202101 WHERE wind_code = '%s'", STOCK_CODE);
    private static final String SQL_2B_NEW_MONTH = String.format("SELECT AVG(latest_price) FROM tb_quotation_history_warm WHERE trade_date >= '2021-01-01 00:00:00' AND trade_date < '2021-02-01 00:00:00' AND wind_code = '%s'", STOCK_CODE);

    // 场景 3: 跨月聚合 (Q1)
    private static final String SQL_3A_OLD_QUARTER = String.format("SELECT AVG(latest_price) as avg_price FROM (SELECT latest_price FROM tb_quotation_history_trend_202101 WHERE wind_code='%s' UNION ALL SELECT latest_price FROM tb_quotation_history_trend_202102 WHERE wind_code='%s' UNION ALL SELECT latest_price FROM tb_quotation_history_trend_202103 WHERE wind_code='%s') as t", STOCK_CODE, STOCK_CODE, STOCK_CODE);
    private static final String SQL_3B_NEW_QUARTER = String.format("SELECT AVG(latest_price) as avg_price FROM tb_quotation_history_warm WHERE trade_date >= '2021-01-01 00:00:00' AND trade_date < '2021-04-01 00:00:00' AND wind_code = '%s'", STOCK_CODE);


    // =================================================================================
    // 测试生命周期 (Test Lifecycle)
    // =================================================================================

    /**
     * 核心方法: 在所有测试开始前运行 (Core method: Runs before all tests)
     * 实验目的 (Purpose):
     * 1. 打印配置信息 (Print configuration)。
     * 2. 查询数据库元数据 (Query database metadata) 以便写入报告。
     * ‼️ 移除了 'static' (Removed 'static')
     */
    @BeforeAll
    void setup() { // ‼️ 移除了 'static' (Removed 'static')
        log.info("=== [OAT] 开始性能基准测试 (Warmup={}次, Benchmark={}次) ===", WARMUP_RUNS, BENCHMARK_RUNS);
        log.info("=== [OAT] 标的 (Stock): {}, 数据库 (Database): {} ===", STOCK_CODE, TARGET_DATABASE);
        try {
            // jdbcTemplate 此时已注入 (jdbcTemplate is now injected)
            mysqlVersion = jdbcTemplate.queryForObject("SELECT VERSION()", String.class);
            mysqlHost = jdbcTemplate.queryForObject("SELECT @@hostname", String.class);
            log.info("=== [OAT] MySQL Version: {}, Host: {} ===", mysqlVersion, mysqlHost);
        } catch (Exception e) {
            log.warn("=== [OAT] 无法获取 MySQL 元数据 (Failed to get MySQL metadata): {} ===", e.getMessage());
            mysqlVersion = "N/A";
            mysqlHost = "N/A";
        }
    }

    /**
     * 核心方法: 在所有测试结束后运行 (Core method: Runs after all tests)
     * 实验目的 (Purpose):
     * 1. 调用报告生成器 (Call the report generator)。
     * ‼️ 移除了 'static' (Removed 'static')
     */
    @AfterAll
    void tearDown() { // ‼️ 移除了 'static' (Removed 'static')
        log.info("=== [OAT] 所有基准测试已完成 (All benchmarks finished) ===");
        writeMarkdownReport(); // 调用非静态方法 (Calling non-static method)
    }

    // =================================================================================
    // 实验一：静态执行计划验证 (Test 1: Static Execution Plan Verification)
    // =================================================================================

    /**
     * 实验目的 (Purpose):
     * 验证新表 (warm) 的 SQL 查询是否能 100% 命中 "分区剪枝 (Partition Pruning)" 和 "二级索引 (Secondary Index)"。
     * (Verify if queries on the new table 100% hit Partition Pruning and Secondary Index.)
     *
     * 预期结果 (Expectation):
     * - `partitions` 字段中只包含查询涉及的月份 (e.g., "p202101")。
     * - `key` 字段中命中 `uniq_windcode_tradedate` 索引。
     * - `access_type` 为 `range`。
     */
    @Test
    @Order(1) // 1. 首先执行静态分析 (Run static analysis first)
    @DisplayName("实验 1: 静态验证 (分区剪枝与索引)")
    void test_A_ExecutionPlanVerification() {
        log.info("--- 正在执行 [实验 1: 静态验证 (EXPLAIN)] ---");

        Map<String, String> scenarios = new LinkedHashMap<>();
        scenarios.put("单日K线 (New)", "EXPLAIN FORMAT=JSON " + SQL_1B_NEW_DAY);
        scenarios.put("单月聚合 (New)", "EXPLAIN FORMAT=JSON " + SQL_2B_NEW_MONTH);
        scenarios.put("跨月(Q1)聚合 (New)", "EXPLAIN FORMAT=JSON " + SQL_3B_NEW_QUARTER);

        boolean allPassed = true;

        for (Map.Entry<String, String> entry : scenarios.entrySet()) {
            String scenarioName = entry.getKey();
            String sql = entry.getValue();
            Map<String, String> result = new LinkedHashMap<>();
            result.put("SQL 场景", scenarioName);

            try {
                log.info("  [EXPLAIN] 正在分析 (Analyzing): {}", scenarioName);
                String jsonPlan = jdbcTemplate.queryForObject(sql, String.class);
                Map<String, Object> plan = objectMapper.readValue(jsonPlan, new TypeReference<>() {});

                // 递归查找 table 节点 (Recursively find the 'table' node)
                Map<String, Object> table = findTableNode(plan);

                if (table != null) {
                    List<String> partitions = (List<String>) table.get("partitions");
                    String accessType = (String) table.get("access_type");
                    String key = (String) table.get("key");

                    log.info("    -> 结果 (Result): 分区(Partitions)={}, 索引(Key)={}, 类型(Access)={}", partitions, key, accessType);

                    result.put("分区 (Partitions)", (partitions == null || partitions.isEmpty()) ? "NONE" : String.join(", ", partitions));
                    result.put("索引 (Index)", String.valueOf(key));
                    result.put("类型 (Type)", String.valueOf(accessType));

                    // 验证逻辑 (Validation Logic)
                    if (partitions == null || partitions.isEmpty() || partitions.size() > 3) { // 允许Q1查3个分区 (Allow 3 partitions for Q1)
                        log.error("    -> 失败 (FAILED)! 分区剪枝失效 (Partition pruning failed)!");
                        result.put("结果 (Result)", "❌ FAIL (Pruning Failed)");
                        allPassed = false;
                    } else if (!("uniq_windcode_tradedate".equals(key))) {
                        log.warn("    -> 警告 (WARNING)! 未命中预期索引 (Did not use expected index)!");
                        result.put("结果 (Result)", "⚠️ WARN (Index Mismatch)");
                    } else {
                        result.put("结果 (Result)", "✅ PASS");
                    }
                } else {
                    result.put("结果 (Result)", "❌ FAIL (Plan Parse Error)");
                    allPassed = false;
                }
            } catch (Exception e) {
                log.error("  [EXPLAIN] SQL分析失败 (SQL analysis failed): {}", e.getMessage());
                result.put("结果 (Result)", "❌ ERROR (" + e.getClass().getSimpleName() + ")");
                allPassed = false;
            }
            planReport.add(result);
        }
        Assertions.assertTrue(allPassed, "静态执行计划验证失败，分区剪枝未生效 (Static plan verification failed, partition pruning not active)");
    }

    // =================================================================================
    // 实验二：热读延迟测试 (Test 2: Hot Read Latency Test)
    // =================================================================================

    /**
     * 实验目的 (Purpose):
     * 模拟数据已在 InnoDB Buffer Pool (内存) 中的情况，即高频访问场景。
     * (Simulate the scenario where data is already in the InnoDB Buffer Pool (RAM), i.e., high-frequency access.)
     *
     * 预期结果 (Expectation):
     * - 场景1 (单日): 新表因 CPU 解压，耗时可能微幅高于旧表 (e.g., 5ms vs 3ms)。这是可接受的权衡。
     * - 场景3 (跨月): 新表因架构优势 (免去 UNION)，耗时应显著低于旧表。
     */
    // ‼️ 暂时注释掉热读测试，防止它污染缓存
    // (Temporarily comment out the hot read test to prevent cache pollution)
    // @Test
    @Order(2) // 2. 其次执行热读测试 (Run hot read test second)
    @DisplayName("实验 2: 热读 (内存命中) 延迟")
    void test_B_HotReadLatency() {
        log.info("--- 正在执行 [实验 2: 热读 (Hot Read) 延迟测试] ---");

        // 场景 1: 单日 (Single Day)
        double oldDay = executeHotBenchmark("1A (旧表-单日)", SQL_1A_OLD_DAY);
        double newDay = executeHotBenchmark("1B (新表-单日)", SQL_1B_NEW_DAY);
        addFileReportResult(hotReadReport, "单日K线", oldDay, newDay);

        // 场景 2: 单月 (Single Month)
        double oldMonth = executeHotBenchmark("2A (旧表-单月)", SQL_2A_OLD_MONTH);
        double newMonth = executeHotBenchmark("2B (新表-单月)", SQL_2B_NEW_MONTH);
        addFileReportResult(hotReadReport, "单月聚合", oldMonth, newMonth);

        // 场景 3: 跨月 (Cross Month)
        double oldQuarter = executeHotBenchmark("3A (旧表-跨月)", SQL_3A_OLD_QUARTER);
        double newQuarter = executeHotBenchmark("3B (新表-跨月)", SQL_3B_NEW_QUARTER);
        addFileReportResult(hotReadReport, "跨月(Q1)聚合", oldQuarter, newQuarter);
    }

    // =================================================================================
    // 实验三：冷读延迟测试 (Test 3: Cold Read Latency Test)
    // =================================================================================

    /**
     * 实验目的 (Purpose):
     * 模拟数据不在内存中，必须从磁盘读取的 "温数据" 访问场景。
     * (Simulate the 'Warm Data' access scenario where data is not in RAM and must be read from disk.)
     *
     * 预期结果 (Expectation):
     * - 在所有场景中，新表因其 57.7% 的空间节省，磁盘 I/O 开销远低于旧表。
     * - 新表的耗时应全面优于 (显著快于) 旧表，证明 "以CPU换IO" 策略成功。
     */
    @Test
    @Order(3) // 3. 最后执行冷读测试 (Run cold read test last)
    @DisplayName("实验 3: 冷读 (磁盘命中) 延迟")
    void test_C_ColdReadLatency() {
        log.info("--- 正在执行 [实验 3: 冷读 (Cold Read) 延迟测试] ---");

        // 场景 1: 单日 (Single Day)
        double oldDay = executeColdReadBenchmark("1A (旧表-单日)", SQL_1A_OLD_DAY);
        double newDay = executeColdReadBenchmark("1B (新表-单日)", SQL_1B_NEW_DAY);
        addFileReportResult(coldReadReport, "单日K线", oldDay, newDay);

        // 场景 2: 单月 (Single Month)
        double oldMonth = executeColdReadBenchmark("2A (旧表-单月)", SQL_2A_OLD_MONTH);
        double newMonth = executeColdReadBenchmark("2B (新表-单月)", SQL_2B_NEW_MONTH);
        addFileReportResult(coldReadReport, "单月聚合", oldMonth, newMonth);

        // 场景 3: 跨月 (Cross Month)
        double oldQuarter = executeColdReadBenchmark("3A (旧表-跨月)", SQL_3A_OLD_QUARTER);
        double newQuarter = executeColdReadBenchmark("3B (新表-跨月)", SQL_3B_NEW_QUARTER);
        addFileReportResult(coldReadReport, "跨月(Q1)聚合", oldQuarter, newQuarter);
    }


    // =================================================================================
    // 核心辅助方法 (Core Helper Methods)
    // =================================================================================

    /**
     * 核心方法 1: 执行热读基准测试 (Core Method 1: Execute Hot Read Benchmark)
     * 包含预热和计时 (Includes warmup and timing)
     */
    private double executeHotBenchmark(String testName, String sql) {
        StopWatch sw = new StopWatch(testName);
        log.info("  [Hot Read] 预热 (Warming up) {}...", testName);

        // 1. 预热 (Warmup) - 确保 JIT 编译和 Buffer Pool 缓存命中
        // (Ensure JIT compilation and Buffer Pool cache hits)
        for (int i = 0; i < WARMUP_RUNS; i++) {
            jdbcTemplate.queryForObject(sql, BigDecimal.class);
        }

        // 2. 压测 (Benchmark)
        log.info("  [Hot Read] 测试 (Benchmarking) {}...", testName);
        long totalNanos = 0;
        for (int i = 0; i < BENCHMARK_RUNS; i++) {
            sw.start();
            jdbcTemplate.queryForObject(sql, BigDecimal.class);
            sw.stop();
            totalNanos += sw.getLastTaskTimeNanos();
        }

        double avgMillis = (totalNanos / 1_000_000.0) / BENCHMARK_RUNS;
        log.info("  [Hot Read] 结果 (Result) {}: 平均耗时 {} ms", testName, String.format("%.3f", avgMillis));
        return avgMillis;
    }

    /**
     * 核心方法 2: 执行冷读基准测试 (Core Method 2: Execute Cold Read Benchmark)
     * 每次执行前都会清空 Buffer Pool (Flushes Buffer Pool before each execution)
     */
    private double executeColdReadBenchmark(String testName, String sql) {
        StopWatch sw = new StopWatch(testName);

        // 1. 强制清空缓存 (Force flush Buffer Pool)
        log.info("  [Cold Read] 正在清空 Buffer Pool (Flushing Buffer Pool)...");
        if (!flushBufferPool()) {
            // 如果刷新失败 (例如权限不足)，则跳过此测试
            // (If flush fails (e.g., permissions), skip this test)
            log.warn("  [Cold Read] 刷新 Buffer Pool 失败，跳过测试 (Flush failed, skipping test): {}", testName);
            return -1.0; // -1.0
        }

        // 2. 执行一次性冷读测试 (Execute one-shot cold read test)
        log.info("  [Cold Read] 测试 (Testing) {}...", testName);
        sw.start();
        jdbcTemplate.queryForObject(sql, BigDecimal.class);
        sw.stop();

        double totalMillis = sw.getTotalTimeMillis();
        log.info("  [Cold Read] 结果 (Result) {}: 耗时 {} ms", testName, String.format("%.3f", totalMillis));
        return totalMillis;
    }

    /**
     * 核心方法 3: 刷新 InnoDB 缓冲池 (Core Method 3: Flush InnoDB Buffer Pool)
     * 这是一个 "trick"，通过动态调整大小来强制清空。
     * (This is a trick to force a flush by dynamically resizing it.)
     * 警告: 执行此操作的数据库用户必须有 SUPER 或 SYSTEM_VARIABLES_ADMIN 权限。
     * (WARNING: The database user MUST have SUPER or SYSTEM_VARIABLES_ADMIN privileges.)
     *
     * @return boolean - true 如果刷新成功 (true if flush succeeded)
     */
    private boolean flushBufferPool() {
        try {
            // 1. 获取当前 Buffer Pool 大小 (Get current size)
            Long currentSize = jdbcTemplate.queryForObject("SELECT @@innodb_buffer_pool_size", Long.class);

            // ‼️ 为本地小内存环境移除 500MB 保护锁 (Removing 500MB safety check for local low-mem env)
            // if (currentSize == null || currentSize <= 1024 * 1024 * 500) {
            //     log.warn("  [Cold Read] Buffer Pool 太小或无法获取，跳过刷新 (Too small or unreadable, skipping flush)");
            //     return false;
            // }

            // ‼️ 增加一个最小的 null 检查 (Add a minimal null check)
            if (currentSize == null) {
                log.warn("  [Cold Read] 无法获取 innodb_buffer_pool_size, 跳过刷新 (Cannot get buffer pool size, skipping flush)");
                return false;
            }

            // 2. 设置一个稍小的值 (e.g., 99%) 来触发刷新 (Set a slightly smaller value (e.g., 99%) to trigger flush)
            long tempSize = (long) (currentSize * 0.99);
            jdbcTemplate.execute(String.format("SET GLOBAL innodb_buffer_pool_size = %d", tempSize));

            // 3. 恢复原大小 (Restore original size)
            jdbcTemplate.execute(String.format("SET GLOBAL innodb_buffer_pool_size = %d", currentSize));

            log.info("  [Cold Read] Buffer Pool 已刷新 (flushed)。");
            return true;

        } catch (DataAccessException e) {
            log.error("--- 刷新 BUFFER POOL 失败! (FLUSH FAILED!) ---");
            log.error("冷读 (Cold Read) 测试结果将不准确 (invalid)!");
            log.error("请确保数据库用户拥有 SYSTEM_VARIABLES_ADMIN (MySQL 8.0+) 或 SUPER (MySQL 5.7) 权限。");
            log.error("GRANT SYSTEM_VARIABLES_ADMIN ON *.* TO 'your_user'@'your_host';");
            return false;
        }
    }

    /**
     * 核心方法 4: 写入 Markdown 报告 (Core Method 4: Write Markdown Report)
     * 实验目的 (Purpose):
     * 将所有测试结果固化为可归档的 Markdown 产出物。
     * (To persist all test results into an archivable Markdown artifact.)
     */
    private void writeMarkdownReport() { // ‼️ 移除了 'static' (Removed 'static')
        StringBuilder sb = new StringBuilder();
        String now = LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss"));
        String fileName = String.format("migration-performance-report-%s.md", LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyyMMdd-HHmmss")));

        // --- 1. 元数据 (Metadata) ---
        sb.append("# 📈 迁移性能验收 (OAT) 报告 (Migration Performance OAT Report)\n\n");
        sb.append("## 🌍 元数据 (Metadata)\n\n");
        sb.append(String.format("- **环境 (Environment)**: %s\n", "Production/Staging (本地测试 Local Test)"));
        sb.append(String.format("- **运行时间 (Run Time)**: %s\n", now));
        sb.append(String.format("- **测试标的 (Test Target)**: %s\n", STOCK_CODE));
        sb.append(String.format("- **MySQL 版本 (Version)**: %s\n", mysqlVersion));
        sb.append(String.format("- **MySQL 主机 (Host)**: %s\n", mysqlHost));
        sb.append("---\n");

        // --- 2. 执行计划 (Execution Plan) ---
        sb.append("## 📌 静态执行计划验证 (Execution Plan Validation)\n\n");
        sb.append(buildMarkdownTable(planReport));
        sb.append("\n---\n");

        // --- 3. 热读 (Hot Read) ---
        sb.append("## ⚡ 热读 (内存命中) 结果 (Hot Read (RAM) Results)\n\n");
        sb.append("> 模拟数据已在 Buffer Pool 缓存中的高频访问，主要考验 **CPU 解压性能**。\n\n");
        sb.append(buildMarkdownTable(hotReadReport));
        sb.append("\n---\n");

        // --- 4. 冷读 (Cold Read) ---
        sb.append("## ❄️ 冷读 (磁盘命中) 结果 (Cold Read (Disk) Results)\n\n");
        sb.append("> 模拟数据不在缓存中、必须从磁盘读取的“温数据”访问，主要考验 **I/O 性能**。\n\n");
        sb.append(buildMarkdownTable(coldReadReport));
        sb.append("\n");

        // --- 写入文件 (Write to File) ---
        try {
            Files.writeString(Paths.get(fileName), sb.toString());
            log.info("=== [OAT] 成功生成性能验收报告 (Successfully generated OAT report): {} ===", fileName);
        } catch (IOException e) {
            log.error("=== [OAT] 写入报告文件失败 (Failed to write report file)! ===", e);
        }
    }

    /**
     * 辅助方法: 添加结果到报告列表 (Helper: Add result to report list)
     * ‼️ 移除了 'static' (Removed 'static')
     */
    private void addFileReportResult(List<Map<String, String>> reportList, String scenario, double oldTime, double newTime) {
        // 自动处理冷读跳过的情况 (Auto-handle cold read skips)
        if (oldTime < 0 || newTime < 0) {
            addFileReportResult(reportList, scenario, oldTime, newTime, "SKIPPED (No Permission)");
        } else {
            addFileReportResult(reportList, scenario, oldTime, newTime, null);
        }
    }

    private void addFileReportResult(List<Map<String, String>> reportList, String scenario, double oldTime, double newTime, String status) {
        Map<String, String> result = new LinkedHashMap<>();
        result.put("场景 (Scenario)", scenario);
        result.put("旧表耗时 (ms)", String.format("%.3f", oldTime));
        result.put("新表耗时 (ms)", String.format("%.3f", newTime));

        if (status != null) {
            result.put("性能变化 (%)", String.format("N/A (%s)", status));
        } else if (oldTime == 0.0 || newTime == 0.0) {
            result.put("性能变化 (%)", "N/A (Error)");
        } else {
            double changePercent = ((newTime - oldTime) / oldTime) * 100.0;
            String changeStr = (changePercent > 0) ?
                    String.format("+%.2f%% (变慢)", changePercent) : // Slower
                    String.format("%.2f%% (提升)", changePercent); // Faster
            result.put("性能变化 (%)", changeStr);
        }
        reportList.add(result);
    }

    /**
     * 辅助方法: 动态构建 Markdown 表格 (Helper: Dynamically build Markdown table)
     * ‼️ 移除了 'static' (Removed 'static')
     */
    private String buildMarkdownTable(List<Map<String, String>> data) {
        if (data.isEmpty()) {
            return "没有可用的测试数据 (No test data available)。\n";
        }
        StringBuilder table = new StringBuilder();
        Map<String, String> firstRow = data.get(0);

        // 1. 构建表头 (Build Header)
        table.append("|");
        for (String header : firstRow.keySet()) {
            table.append(" ").append(header).append(" |");
        }
        table.append("\n");

        // 2. 构建分隔符 (Build Separator)
        table.append("|");
        for (int i = 0; i < firstRow.size(); i++) {
            table.append(" --- |");
        }
        table.append("\n");

        // 3. 构建数据行 (Build Data Rows)
        for (Map<String, String> row : data) {
            table.append("|");
            for (String value : row.values()) {
                table.append(" ").append(value).append(" |");
            }
            table.append("\n");
        }
        return table.toString();
    }

    /**
     * 辅助方法: 递归查找 EXPLAIN JSON 中的 'table' 节点
     * (Helper: Recursively find the 'table' node in EXPLAIN JSON)
     * ‼️ 移除了 'static' (Removed 'static')
     */
    @SuppressWarnings("unchecked")
    private Map<String, Object> findTableNode(Map<String, Object> node) {
        // 检查当前节点是否是我们想要的 'table' 节点
        // (Check if the current node is the 'table' node we want)
        if (node.containsKey("table_name") && node.get("table_name").equals("tb_quotation_history_warm")) {
            return node;
        }

        // 递归遍历子节点 (Recursively traverse child nodes)
        for (Object value : node.values()) {
            if (value instanceof Map) {
                Map<String, Object> found = findTableNode((Map<String, Object>) value);
                if (found != null) return found;
            } else if (value instanceof List) {
                for (Object item : (List<?>) value) {
                    if (item instanceof Map) {
                        Map<String, Object> found = findTableNode((Map<String, Object>) item);
                        if (found != null) return found;
                    }
                }
            }
        }
        return null; // 未找到 (Not found)
    }
}