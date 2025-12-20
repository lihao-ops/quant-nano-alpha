package com.hao.strategyengine;

import com.alibaba.cloud.nacos.NacosConfigManager;
import com.alibaba.nacos.api.config.ConfigService;
import com.alibaba.nacos.api.config.listener.Listener;
import lombok.extern.slf4j.Slf4j;
import org.mybatis.spring.annotation.MapperScan;
import org.springframework.boot.ApplicationRunner;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cloud.client.discovery.EnableDiscoveryClient;
import org.springframework.cloud.openfeign.EnableFeignClients;
import org.springframework.context.annotation.Bean;
import org.springframework.scheduling.annotation.EnableScheduling;

import java.util.concurrent.Executor;
import java.util.concurrent.Executors;

/**
 * 策略引擎服务启动类
 *
 * 设计目的：
 * 1. 启动策略引擎服务并加载Spring上下文。
 * 2. 注册Nacos配置监听，感知配置变更。
 *
 * 为什么需要该类：
 * - 作为服务入口统一承载启动与配置治理流程。
 *
 * 核心实现思路：
 * - 通过ApplicationRunner注册配置监听器并记录变更日志。
 */
@Slf4j
@MapperScan("com.hao.strategyengine.integration.db.mapper")
@EnableFeignClients//开启Feign远程调用功能
@EnableDiscoveryClient//开启服务发现功能
@EnableScheduling//开启定时任务功能
@SpringBootApplication
public class StrategyEngineApplication {

    /**
     * 启动入口
     *
     * 实现逻辑：
     * 1. 启动Spring应用上下文。
     *
     * @param args 启动参数
     */
    public static void main(String[] args) {
        // 实现思路：
        // 1. 交由SpringApplication启动上下文。
        SpringApplication.run(StrategyEngineApplication.class, args);
    }

    // 实现思路：
    // 1. 项目启动后监听配置文件变化。
    // 2. 发生变化后记录日志并预留通知入口。
    @Bean
    ApplicationRunner applicationRunner(NacosConfigManager nacosConfigManager) {
        /**
         * 注册配置监听器
         *
         * 实现逻辑：
         * 1. 获取Nacos配置服务并注册监听器。
         * 2. 输出配置变更日志，便于运维排查。
         *
         * @param nacosConfigManager Nacos配置管理器
         * @return ApplicationRunner回调
         */
        return args -> {
            // 实现思路：
            // 1. 获取配置服务并注册监听器。
            ConfigService configService = nacosConfigManager.getConfigService();
            configService.addListener("application-dev.yml", "quant-strategy-engine", new Listener() {
                @Override
                public Executor getExecutor() {
                    // 实现思路：
                    // 1. 使用独立线程池处理配置回调。
                    return Executors.newFixedThreadPool(2);
                }

                @Override
                public void receiveConfigInfo(String configInfo) {
                    // 实现思路：
                    // 1. 输出配置变更内容。
                    // 2. 预留告警通知入口。
                    log.info("配置监听变更|Config_listener_change,configInfo={}", configInfo);
                    log.info("发送告警邮件|Send_alert_email");
                }
            });
            log.info("启动配置监听|Start_config_listener");
        };
    }
}
