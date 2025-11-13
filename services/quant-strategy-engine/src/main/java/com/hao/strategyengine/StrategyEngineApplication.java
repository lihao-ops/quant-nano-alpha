package com.hao.strategyengine;

/**
 * 类说明 / Class Description:
 * 中文：策略引擎微服务的启动类，负责应用引导、配置监听与基础设施集成（Nacos、Feign、调度、Mapper扫描）。
 * English: Bootstrap class for the strategy engine microservice; handles app startup, config listening, and infrastructure integrations (Nacos, Feign, Scheduling, Mapper scan).
 *
 * 使用场景 / Use Cases:
 * 中文：服务进程入口，注册必要的Spring特性并在启动后监听配置变更以触发告警或通知。
 * English: Service process entry; registers required Spring features and listens for config changes post-startup to trigger alerts or notifications.
 *
 * 设计目的 / Design Purpose:
 * 中文：集中化服务引导与配置监听，提升可观测性与运维效率，确保外部配置变更被及时感知。
 * English: Centralize service bootstrap and config listening to enhance observability and ops efficiency, ensuring timely awareness of external config changes.
 */
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

@Slf4j
@MapperScan("com.hao.strategyengine.integration.db.mapper")
@EnableFeignClients//开启Feign远程调用功能
@EnableDiscoveryClient//开启服务发现功能
@EnableScheduling          // ✅ 开启定时任务功能
@SpringBootApplication
public class StrategyEngineApplication {

    public static void main(String[] args) {
        // 中文：启动Spring应用上下文，加载并初始化所有组件
        // English: Start Spring application context and initialize all components
        SpringApplication.run(StrategyEngineApplication.class, args);
    }

    //1.项目启动就监听配置文件变化
    //2.发生变化后拿到变化的内容
    //3，发送提醒邮件
    @Bean
    ApplicationRunner applicationRunner(NacosConfigManager nacosConfigManager) {
        /**
         * 方法说明 / Method Description:
         * 中文：注册在应用启动后执行的监听器，订阅Nacos配置变更并输出告警日志或通知。
         * English: Register a post-start runner that subscribes to Nacos config changes and emits alert logs or notifications.
         *
         * 参数 / Parameters:
         * @param nacosConfigManager 中文：Nacos配置管理器，用于获取ConfigService / English: Nacos config manager to obtain ConfigService
         *
         * 返回值 / Return:
         * 中文：ApplicationRunner 回调，在应用启动后执行 / English: ApplicationRunner callback executed after app startup
         *
         * 异常 / Exceptions:
         * 中文：监听器内部可能抛出运行时异常，需记录日志以便排查 / English: Listener may throw runtime exceptions; should be logged for troubleshooting
         */
        return args -> {
            // 中文：从管理器获取配置服务实例
            // English: Obtain config service instance from manager
            ConfigService configService = nacosConfigManager.getConfigService();
            configService.addListener("application-dev.yml", "quant-strategy-engine", new Listener() {
                @Override
                public Executor getExecutor() {
                    // 中文：配置监听线程池，避免阻塞主线程
                    // English: Configure listener thread pool to avoid blocking main thread
                    return Executors.newFixedThreadPool(2);
                }

                @Override
                public void receiveConfigInfo(String configInfo) {
                    // 中文：接收配置变更内容并输出双语日志，后续可改为邮件或IM通知
                    // English: Receive config change content and log bilingually; can be extended to email/IM notification
                    log.info("nacosConfig_conversionInfo_listener_info={}", configInfo);
                    log.info("to email!");
                }
            });
            // 中文：启动完成后提示监听器已就绪
            // English: Indicate listener is ready after startup completes
            log.info("start_nacosConfig_conversionInfo_listener!");
        };
    }
}
