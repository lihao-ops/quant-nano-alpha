package com.hao.datacollector;

import com.alibaba.cloud.nacos.NacosConfigManager;
import com.alibaba.nacos.api.config.ConfigService;
import com.alibaba.nacos.api.config.listener.Listener;
import lombok.extern.slf4j.Slf4j;
import org.mybatis.spring.annotation.MapperScan;
import org.springframework.boot.ApplicationRunner;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cache.annotation.EnableCaching;
import org.springframework.cloud.client.discovery.EnableDiscoveryClient;
import org.springframework.context.annotation.Bean;
import org.springframework.retry.annotation.EnableRetry;

import java.util.concurrent.Executor;
import java.util.concurrent.Executors;

@Slf4j
@MapperScan("com.hao.datacollector.dal.dao")
@SpringBootApplication
@EnableDiscoveryClient//开启服务发现功能
@EnableCaching//缓存
@EnableRetry
public class DataCollectorApplication {

    public static void main(String[] args) {
        SpringApplication.run(DataCollectorApplication.class, args);
    }

    //1.项目启动就监听配置文件变化
    //2.发生变化后拿到变化的内容
    //3，发送提醒邮件
    @Bean
    ApplicationRunner applicationRunner(NacosConfigManager nacosConfigManager) {
        return args -> {
            ConfigService configService = nacosConfigManager.getConfigService();
            configService.addListener("application-dev.yml", "quant-data-collector", new Listener() {
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
