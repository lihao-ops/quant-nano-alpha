package com.hao.riskcontrol;

import lombok.extern.slf4j.Slf4j;
import org.mybatis.spring.annotation.MapperScan;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cache.annotation.EnableCaching;
import org.springframework.cloud.client.discovery.EnableDiscoveryClient;

/**
 * @author hli
 * @program: quant-nano-alpha
 * @Date 2025-08-24 21:00:53
 * @description: 风控微服务启动类
 */
@Slf4j
@MapperScan("com.hao.riskcontrol.dal.dao")
@SpringBootApplication
@EnableDiscoveryClient//开启服务发现功能
@EnableCaching//缓存
//@EnableRetry
public class RiskControlApplication {
    public static void main(String[] args) {
        SpringApplication.run(RiskControlApplication.class, args);
    }
}
