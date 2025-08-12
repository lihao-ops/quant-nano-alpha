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
 * @author xuxueli 2018-10-28 00:38:13
 */
@Slf4j
@EnableDiscoveryClient//开启服务发现功能
@SpringBootApplication
public class XxlJobAdminApplication {

    public static void main(String[] args) {
        SpringApplication.run(XxlJobAdminApplication.class, args);
    }

    //1.项目启动就监听配置文件变化
    //2.发生变化后拿到变化的内容
    //3，发送提醒邮件
    @Bean
    ApplicationRunner applicationRunner(NacosConfigManager nacosConfigManager) {
        return args -> {
            ConfigService configService = nacosConfigManager.getConfigService();
            configService.addListener("application-dev.yml", "quant-xxl-job", new Listener() {
                @Override
                public Executor getExecutor() {
                    return Executors.newFixedThreadPool(2);
                }

                @Override
                public void receiveConfigInfo(String configInfo) {
                    log.info("nacosConfig_conversionInfo_listener_info={}", configInfo);
                    log.info("to email!");
                }
            });
            log.info("start_nacosConfig_conversionInfo_listener!");
        };
    }
}