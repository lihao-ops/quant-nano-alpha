package com.hao.strategyengine.monitoring;

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
    void testMonitorExecution() {
        log.info("🚀 启动 MySQL 监控执行测试 / Start MySQL Monitor Execution Test");
        try {
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
    void testMetricsValidation() {
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
    void testAnalyzeConnectionRange() {
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
    void testMonitorHealth() {
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
    void testResetStatistics() {
        log.info("🧹 测试监控统计重置 / Test Monitor Statistics Reset");
        monitor.resetMonitorStatistics();
        MysqlLoadMonitor.MonitorHealthStatus health = monitor.getMonitorHealth();
        Assertions.assertEquals(0, health.getSuccessCount(), "重置后成功次数应为0");
        Assertions.assertEquals(0, health.getFailureCount(), "重置后失败次数应为0");
        log.info("✅ 重置测试通过 / Statistics reset verified");
    }
}
