package com.hao.quant.stocklist;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cache.annotation.EnableCaching;
import org.springframework.kafka.annotation.EnableKafka;
import org.springframework.scheduling.annotation.EnableScheduling;

/**
 * 稳定精选股票查询服务入口。
 */
@EnableKafka
@EnableCaching
@EnableScheduling
@SpringBootApplication
public class QuantStockListApplication {

    public static void main(String[] args) {
        SpringApplication.run(QuantStockListApplication.class, args);
    }
}
