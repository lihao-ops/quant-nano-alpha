package com.hao.strategyengine.monitoring;

/**
 * 测试目的 / Test Purpose:
 * 中文：验证 MySQL 负载监控组件的关键功能与健康评估逻辑，确保指标采集、区间分析与统计重置行为符合预期。
 * English: Validate key functions of the MySQL load monitoring component, ensuring metric collection, range analysis, and statistics reset behave as expected.
 *
 * 预期结果 / Expected Result:
 * 中文：所有测试应成功执行；在无真实数据库时以跳过处理，日志输出包含中英双语提示；区间分析返回非空建议文本；统计重置后计数归零。
 * English: All tests execute successfully; when no real DB is available tests are skipped with bilingual logs; range analysis returns non-null advice text; counters reset to zero.
 *
 * 执行方式 / How to Execute:
 * 中文：在 dev/test 环境运行单元测试；确保 application.yml 配置正确；可通过 IDE 或命令行执行。
 * English: Run unit tests in dev/test environment; ensure application.yml is configured correctly; run via IDE or CLI.
 */
import com.hao.strategyengine.monitoring.mysql.MysqlLoadMonitor;
import lombok.extern.slf4j.Slf4j;
import org.junit.jupiter.api.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

/**
 * ==========================================================
 * 🧩 MySQL监控单元测试 (MySQL Load Monitor Integration Test)
 * ==========================================================
 * 【测试目标 / Purpose】
 * ✅ 验证 MysqlLoadMonitor 监控组件的执行逻辑是否稳定
 * ✅ 检查核心指标：Threads_connected / Threads_running / max_connections
 * ✅ 验证连接使用率、线程压力与健康区间判断逻辑
 * ✅ 输出带中英文提示的日志，便于团队协作与调优
 * <p>
 * 【执行说明 / Instructions】
 * - 建议在 dev 或 test 环境执行，不影响生产数据库。
 * - application.yml 中需配置正确的数据源。
 * - 若未连接真实 MySQL，会提示但不失败。
 * <p>
 * 【最佳实践】
 * 在压测、巡检前执行此测试，快速了解数据库连接健康度。
 * ==========================================================
 */
@Slf4j
@SpringBootTest
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
public class MysqlLoadMonitorTest {

    @Autowired
    private MysqlLoadMonitor monitor;

    /**
     * 【1️⃣ 基础执行验证】
     * ----------------------------------------------------------
     * 确认 monitor.monitor() 方法能顺利运行且无异常抛出。
     */
    @Test
    @Order(1)
    /**
     * 方法说明 / Method Description:
     * 中文：验证监控核心方法可正常执行且不抛异常。
     * English: Verify monitor core method executes without throwing exceptions.
     *
     * 参数 / Parameters:
     * @param 无 中文说明：无入参 / English: none
     *
     * 返回值 / Return:
     * 中文：无返回值，通过日志与断言判断用例是否通过 / English: void; pass judged via logs and assertions
     *
     * 异常 / Exceptions:
     * 中文：捕获所有异常并断言失败，以便快速定位问题 / English: Catch any exception and assert failure for quick troubleshooting
     */
    void testMonitorExecution() {
        // 中文：启动监控执行并观察是否出现异常
        // English: Start monitor execution and observe for exceptions
        log.info("🚀 启动 MySQL 监控执行测试 / Start MySQL Monitor Execution Test");
        try {
            // 中文：调用监控入口以触发指标采集
            // English: Invoke monitor entry to collect metrics
            monitor.monitor();
            log.info("✅ 监控任务执行成功，无异常抛出 / Monitor executed successfully without exceptions.");
        } catch (Exception e) {
            log.error("❌ 监控执行过程中发生异常 / Exception occurred during monitoring", e);
            Assertions.fail("监控任务执行失败 / Monitoring task failed: " + e.getMessage());
        }
    }

    /**
     * 【2️⃣ 指标完整性校验】
     * ----------------------------------------------------------
     * 验证 Threads_connected / Threads_running / max_connections 的取值合理性。
     */
    @Test
    @Order(2)
    /**
     * 方法说明 / Method Description:
     * 中文：校验关键指标合理性，包括连接数、运行线程与最大连接。
     * English: Validate reasonableness of key metrics: connections, running threads, and max connections.
     *
     * 参数 / Parameters:
     * @param 无 中文说明：无入参 / English: none
     *
     * 返回值 / Return:
     * 中文：无返回值，通过断言与日志进行验证 / English: void; validation via assertions and logs
     *
     * 异常 / Exceptions:
     * 中文：如无真实数据库则跳过测试并记录双语日志 / English: Skip when no real DB, with bilingual logs
     */
    void testMetricsValidation() {
        // 中文：采集指标并进行范围与关系校验
        // English: Collect metrics and validate ranges and relationships
        log.info("🔍 开始 MySQL 指标校验 / Start MySQL Metrics Validation");
        try {
            long threadsConnected = monitor.queryMetricValue("Threads_connected");
            long threadsRunning = monitor.queryMetricValue("Threads_running");
            long maxConnections = monitor.queryMetricValue("max_connections");

            log.info("当前连接数 (Threads_connected): {}", threadsConnected);
            log.info("当前运行线程数 (Threads_running): {}", threadsRunning);
            log.info("最大连接数 (max_connections): {}", maxConnections);

            Assertions.assertTrue(maxConnections > 0, "max_connections must be > 0");
            Assertions.assertTrue(threadsConnected >= 0, "Threads_connected should be non-negative");
            Assertions.assertTrue(threadsConnected <= maxConnections,
                    "Threads_connected should not exceed max_connections");

            double usage = (double) threadsConnected / maxConnections * 100;
            log.info("当前连接使用率 (Connection Usage): {}%", String.format("%.2f", usage));

            if (usage < 70) {
                log.info("✅ 连接使用率健康 / Connection usage within healthy range (<70%)");
            } else if (usage < 90) {
                log.warn("⚠️ 连接使用率较高 / Connection usage high ({}%)", String.format("%.2f", usage));
            } else {
                log.error("🚨 连接数接近上限 / Connections nearly exhausted ({}%)", String.format("%.2f", usage));
            }

        } catch (Exception e) {
            log.error("⚠️ 无法验证监控指标，可能测试环境未连接真实数据库 / Failed to validate metrics", e);
            Assumptions.abort("跳过：测试环境未连接数据库 / Skipped due to missing DB connection");
        }
    }

    /**
     * 【3️⃣ 连接区间分析验证】
     * ----------------------------------------------------------
     * 调用 analyzeOptimalConnectionRange() 输出推荐区间与优化建议。
     * 用于确认算法逻辑正确且日志输出清晰。
     */
    @Test
    @Order(3)
    /**
     * 方法说明 / Method Description:
     * 中文：调用区间分析方法，确认返回建议文本与异常处理正常。
     * English: Call range analysis method and confirm advice text returned and exception handled correctly.
     *
     * 参数 / Parameters:
     * @param 无 中文说明：无入参 / English: none
     *
     * 返回值 / Return:
     * 中文：无返回值，通过非空断言判定分析结果有效 / English: void; non-null assertion indicates valid analysis
     *
     * 异常 / Exceptions:
     * 中文：捕获异常并断言失败，便于定位问题 / English: Catch exceptions and assert failure for troubleshooting
     */
    void testAnalyzeConnectionRange() {
        // 中文：执行连接区间分析以输出优化建议
        // English: Execute connection range analysis to output optimization advice
        log.info("📊 开始 MySQL 连接区间分析测试 / Start MySQL Connection Range Analysis Test");
        try {
            String result = monitor.analyzeOptimalConnectionRange();
            Assertions.assertNotNull(result, "连接区间分析结果不应为空 / Result should not be null");
            log.info("✅ 分析结果输出成功：\n{}", result);
        } catch (Exception e) {
            log.error("❌ 连接区间分析失败 / Connection range analysis failed", e);
            Assertions.fail("分析执行异常: " + e.getMessage());
        }
    }

    /**
     * 【4️⃣ 监控器健康状态验证】
     * ----------------------------------------------------------
     * 测试 getMonitorHealth() 输出是否合理，包括成功率、健康等级等。
     */
    @Test
    @Order(4)
    /**
     * 方法说明 / Method Description:
     * 中文：校验健康状态输出是否合理，包括健康等级与核心设备信息。
     * English: Validate health status output including health level and core device info.
     *
     * 参数 / Parameters:
     * @param 无 中文说明：无入参 / English: none
     *
     * 返回值 / Return:
     * 中文：无返回值，通过断言验证健康对象与关键字段 / English: void; assertions validate health object and key fields
     *
     * 异常 / Exceptions:
     * 中文：无特殊异常，按分支记录状态日志 / English: No special exceptions; log status by branch
     */
    void testMonitorHealth() {
        // 中文：获取健康状态对象并检查指标合理性
        // English: Fetch health status object and check metric reasonableness
        log.info("🧩 开始监控器健康状态验证 / Start Monitor Health Check");
        MysqlLoadMonitor.MonitorHealthStatus health = monitor.getMonitorHealth();

        log.info("健康状态报告 / Health Report: {}", health);
        Assertions.assertNotNull(health, "健康状态不应为空 / Health status should not be null");
        Assertions.assertTrue(health.getCpuCores() > 0, "CPU核心数应大于0 / CPU cores must be > 0");

        switch (health.getHealthLevel()) {
            case "HEALTHY" -> log.info("✅ 监控器状态良好 / Monitor is healthy");
            case "WARNING" -> log.warn("⚠️ 监控器状态告警 / Monitor warning state");
            case "CRITICAL" -> log.error("🚨 监控器状态严重异常 / Monitor critical state");
            default -> log.info("ℹ️ 未知状态 / Unknown health level");
        }
    }

    /**
     * 【5️⃣ 监控统计重置验证】
     * ----------------------------------------------------------
     * 测试 resetMonitorStatistics() 能否正确清理统计数据。
     */
    @Test
    @Order(5)
    /**
     * 方法说明 / Method Description:
     * 中文：验证统计重置功能，确保计数清零并无副作用。
     * English: Validate statistics reset function, ensuring zero counters and no side effects.
     *
     * 参数 / Parameters:
     * @param 无 中文说明：无入参 / English: none
     *
     * 返回值 / Return:
     * 中文：无返回值，通过断言验证重置结果 / English: void; assertions verify reset outcome
     *
     * 异常 / Exceptions:
     * 中文：无特殊异常，失败时给出明确断言信息 / English: No special exceptions; failures provide clear assertion messages
     */
    void testResetStatistics() {
        // 中文：执行重置并验证计数器归零
        // English: Execute reset and verify counters zeroed
        log.info("🧹 测试监控统计重置 / Test Monitor Statistics Reset");
        monitor.resetMonitorStatistics();
        MysqlLoadMonitor.MonitorHealthStatus health = monitor.getMonitorHealth();
        Assertions.assertEquals(0, health.getSuccessCount(), "重置后成功次数应为0");
        Assertions.assertEquals(0, health.getFailureCount(), "重置后失败次数应为0");
        log.info("✅ 重置测试通过 / Statistics reset verified");
    }
}
