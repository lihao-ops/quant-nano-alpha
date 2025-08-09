package com.hao.datacollector;

import org.mybatis.spring.annotation.MapperScan;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cache.annotation.EnableCaching;
import org.springframework.retry.annotation.EnableRetry;

@MapperScan("com.hao.datacollector.dal.dao")
@SpringBootApplication
@EnableCaching//缓存
@EnableRetry
public class DataCollectorApplication {

    public static void main(String[] args) {
        SpringApplication.run(DataCollectorApplication.class, args);
    }

}
