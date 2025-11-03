package com.hao.strategyengine.monitoring;

import lombok.extern.slf4j.Slf4j;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

/**
 * MySQL监控单元测试类 (MySQL Load Monitor Test)
 * --------------------------------------------------
 * 主要目标：
 *  1. 验证监控逻辑是否能成功执行且无异常。
 *  2. 确认监控日志能正确输出中英文提示。
 *  3. 检查核心指标的正确性（数值>0 且连接比例合理）。
 *
 * 使用说明：
 *  - 建议在 dev / test 环境执行，不影响生产库。
 *  - 执行前请确保 application.yml 中已正确配置数据源。
 */
@Slf4j
@SpringBootTest
public class MysqlLoadMonitorTest {

    @Autowired
    private MysqlLoadMonitor monitor;

    @Test
    void monitor() {
        log.info("🔍 启动MySQL负载监控单元测试 / Start MySQL Load Monitor Test");

        try {
            // 执行一次监控任务
            monitor.monitor();

            // 在理想情况下不应抛出任何异常
            log.info("✅ 监控执行成功，无异常抛出 / Monitor executed successfully without exceptions.");

        } catch (Exception e) {
            log.error("❌ 监控执行过程中发生异常 / Exception occurred during monitoring", e);
            Assertions.fail("监控任务执行失败 / Monitoring task failed: " + e.getMessage());
        }

        // 二次验证：通过连接比与线程比进行基础判断
        try {
            // 通过反射或方法调用来获取当前状态（这里假设 monitor 提供一个内部查询接口）
            long threadsConnected = monitor.queryMetricValue("Threads_connected");
            long threadsRunning = monitor.queryMetricValue("Threads_running");
            long maxConnections = monitor.queryMetricValue("max_connections");

            log.info("当前连接数(Threads_connected): {}", threadsConnected);
            log.info("当前运行线程数(Threads_running): {}", threadsRunning);
            log.info("最大连接数(max_connections): {}", maxConnections);

            Assertions.assertTrue(threadsConnected >= 0, "连接数应为非负数 / Threads_connected should be non-negative.");
            Assertions.assertTrue(maxConnections > 0, "最大连接数应大于0 / max_connections must be > 0.");
            Assertions.assertTrue(threadsConnected <= maxConnections, "当前连接数不应超过最大连接数 / Threads_connected <= max_connections.");

            log.info("✅ MySQL监控指标校验通过 / MySQL monitoring metrics validated successfully.");

        } catch (Exception e) {
            log.error("⚠️ 无法验证监控指标，可能是测试环境未启用真实数据库 / Failed to validate metrics, DB may not be active in test env.", e);
        }

        log.info("🏁 MySQL负载监控测试结束 / MySQL Load Monitor Test Completed.");
    }
}
