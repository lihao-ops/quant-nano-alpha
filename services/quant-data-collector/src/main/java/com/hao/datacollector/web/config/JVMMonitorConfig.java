package com.hao.datacollector.web.config;

import com.hao.datacollector.web.handler.JVMMonitor;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.annotation.EnableScheduling;
import org.springframework.scheduling.annotation.Scheduled;

/**
 * JVM 监控配置类
 *
 * <p>功能：
 * - 开启定时任务
 * - 每 10 秒执行一次 JVM 资源检查
 */
@Configuration
@EnableScheduling
public class JVMMonitorConfig {

    private final JVMMonitor jvmMonitor;

    public JVMMonitorConfig(JVMMonitor jvmMonitor) {
        this.jvmMonitor = jvmMonitor;
    }

    /**
     * 定时检查 JVM 内存和线程状态
     * cron 或 fixedRate 可以根据需求调整
     */
//    @Scheduled(fixedRate = 10000) // 每 10 秒执行一次
    @Scheduled(fixedRate = 1000)
    public void monitor() {
        jvmMonitor.checkJVM();
    }
}
