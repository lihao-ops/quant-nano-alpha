package com.quant.data.archive;

import org.mybatis.spring.annotation.MapperScan;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cloud.client.discovery.EnableDiscoveryClient;
import org.springframework.scheduling.annotation.EnableScheduling;

/**
 * 量化数据归档服务应用入口
 * 功能覆盖：
 * - 行情数据转储
 * - 实时数据落盘
 * - 历史数据归档
 * - 日志/审计数据归档
 * - 冷数据分层
 */
@SpringBootApplication
@EnableDiscoveryClient
@EnableScheduling
@MapperScan("com.quant.data.archive.mapper")
public class DataArchiveApplication {
    public static void main(String[] args) {
        SpringApplication.run(DataArchiveApplication.class, args);
    }
}
