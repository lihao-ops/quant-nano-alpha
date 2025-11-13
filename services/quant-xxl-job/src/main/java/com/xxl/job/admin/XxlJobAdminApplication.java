package com.xxl.job.admin;

/**
 * 类说明 / Class Description:
 * 中文：XXL-Job 管理端启动类，负责应用引导与Nacos配置监听注册，支持服务发现。
 * English: XXL-Job admin bootstrap class; starts application and registers Nacos config listener, with service discovery enabled.
 *
 * 使用场景 / Use Cases:
 * 中文：作为进程入口，初始化Spring上下文，订阅配置变更以提升可观测性与运维效率。
 * English: Process entry to initialize Spring context and subscribe to config changes for better observability and ops efficiency.
 *
 * 设计目的 / Design Purpose:
 * 中文：集中化应用启动与外部配置监听，保障配置变化的及时感知与响应。
 * English: Centralize app startup and external config listening to ensure timely awareness and response to changes.
 */

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
 * @author xuxueli 2018-10-28 00:38:13
 */
@Slf4j
@EnableDiscoveryClient//开启服务发现功能
@SpringBootApplication
public class XxlJobAdminApplication {

    public static void main(String[] args) {
        /**
         * 方法说明 / Method Description:
         * 中文：启动Spring Boot应用，加载并初始化管理端组件。
         * English: Start Spring Boot application, load and initialize admin components.
         *
         * 参数 / Parameters:
         * @param args 中文：命令行参数 / English: command-line arguments
         *
         * 返回值 / Return:
         * 中文：无 / English: void
         *
         * 异常 / Exceptions:
         * 中文：启动过程中可能抛出运行时异常，框架负责处理 / English: runtime exceptions may occur during startup; handled by framework
         */
        // 中文：启动Spring应用上下文
        // English: Launch Spring application context
        SpringApplication.run(XxlJobAdminApplication.class, args);
    }

    //1.项目启动就监听配置文件变化
    //2.发生变化后拿到变化的内容
    //3，发送提醒邮件
    @Bean
    ApplicationRunner applicationRunner(NacosConfigManager nacosConfigManager) {
        /**
         * 方法说明 / Method Description:
         * 中文：注册启动后执行的配置监听器，订阅Nacos指定配置并输出变更日志。
         * English: Register a post-start config listener to subscribe Nacos config and log changes.
         *
         * 参数 / Parameters:
         * @param nacosConfigManager 中文：Nacos配置管理器 / English: Nacos config manager
         *
         * 返回值 / Return:
         * 中文：ApplicationRunner回调 / English: ApplicationRunner callback
         *
         * 异常 / Exceptions:
         * 中文：可能出现网络或服务异常，需记录日志便于排查 / English: network/service exceptions may occur; log for troubleshooting
         */
        return args -> {
            // 中文：获取配置服务实例
            // English: Obtain config service instance
            ConfigService configService = nacosConfigManager.getConfigService();
            configService.addListener("application-dev.yml", "quant-xxl-job", new Listener() {
                @Override
                public Executor getExecutor() {
                    // 中文：配置监听线程池，避免阻塞主线程
                    // English: Configure listener thread pool to avoid blocking main thread
                    return Executors.newFixedThreadPool(2);
                }

                @Override
                public void receiveConfigInfo(String configInfo) {
                    // 中文：接收配置变更内容并输出双语日志，可扩展为通知机制
                    // English: Receive config change content and log bilingually; can extend to notifications
                    log.info("nacosConfig_conversionInfo_listener_info={}", configInfo);
                    log.info("to email!");
                }
            });
            // 中文：提示监听器已启动
            // English: Indicate listener started
            log.info("start_nacosConfig_conversionInfo_listener!");
        };
    }
}
