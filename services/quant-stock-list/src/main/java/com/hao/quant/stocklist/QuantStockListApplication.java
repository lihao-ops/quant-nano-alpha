package com.hao.quant.stocklist;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cache.annotation.EnableCaching;
import org.springframework.kafka.annotation.EnableKafka;
import org.springframework.scheduling.annotation.EnableScheduling;

/**
 * 稳定精选股票查询服务入口。
 * <p>
 * 应用通过 Spring Boot 启动,整合缓存、Kafka 与调度能力,用于提供稳定策略股票列表的查询能力。
 * </p>
 */
@EnableKafka
@EnableCaching
@EnableScheduling
@SpringBootApplication
public class QuantStockListApplication {

    /**
     * 启动 SpringBoot 应用。
     *
     * @param args 命令行参数
     */
    public static void main(String[] args) {
        SpringApplication.run(QuantStockListApplication.class, args);
    }
}
