package com.xxl.job.admin;

import com.alibaba.cloud.nacos.NacosConfigManager;
import com.alibaba.nacos.api.config.ConfigService;
import com.alibaba.nacos.api.config.listener.Listener;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.ApplicationRunner;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cloud.client.discovery.EnableDiscoveryClient;
import org.springframework.context.annotation.Bean;

import java.util.concurrent.Executor;
import java.util.concurrent.Executors;

/**
 * XXL-Job管理端启动类
 *
 * 设计目的：
 * 1. 作为管理端进程入口，完成Spring上下文启动。
 * 2. 注册Nacos配置监听，感知配置变更。
 *
 * 为什么需要该类：
 * - 管理端需要统一的启动与配置治理入口。
 *
 * 核心实现思路：
 * - 启动时注册ApplicationRunner，监听配置变更并记录日志。
 *
 * @author xuxueli 2018-10-28 00:38:13
 */
@Slf4j
@EnableDiscoveryClient//开启服务发现功能
@SpringBootApplication
public class XxlJobAdminApplication {

    public static void main(String[] args) {
        /**
         * 启动SpringBoot应用
         *
         * 实现逻辑：
         * 1. 启动Spring上下文并初始化管理端组件。
         *
         * @param args 命令行参数
         */
        // 实现思路：
        // 1. 交由SpringApplication启动上下文。
        SpringApplication.run(XxlJobAdminApplication.class, args);
    }

    // 实现思路：
    // 1. 项目启动后监听配置变化。
    // 2. 发生变更时记录日志并预留通知入口。
    @Bean
    ApplicationRunner applicationRunner(NacosConfigManager nacosConfigManager) {
        /**
         * 注册配置监听器
         *
         * 实现逻辑：
         * 1. 获取Nacos配置服务并注册监听器。
         * 2. 输出配置变更日志，便于运维审计。
         *
         * @param nacosConfigManager Nacos配置管理器
         * @return ApplicationRunner回调
         */
        return args -> {
            // 实现思路：
            // 1. 获取配置服务并注册监听器。
            ConfigService configService = nacosConfigManager.getConfigService();
            configService.addListener("application-dev.yml", "quant-xxl-job", new Listener() {
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
