package com.hao.datacollector.cache;

import com.alibaba.fastjson.JSON;
import constants.RedisKeyConstants;
import com.hao.datacollector.integration.redis.RedisClient;
import com.hao.datacollector.service.LimitUpService;
import jakarta.annotation.PostConstruct;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.Map;
import java.util.Set;

/**
 * @author hli
 * @program: datacollector
 * @Date 2025-08-05 18:49:36
 * @description: 涨停相关缓存
 */
@Slf4j
@Component("LimitCache")
public class LimitCache {
    @Autowired
    private LimitUpService limitUpService;

    @Autowired
    private RedisClient<String> redisClient;

    @PostConstruct
    public void initLimitUpMappingStockCache() {
        //key:交易日期,value:当天涨停股票代码Set
        Map<String, Set<String>> limitUpMappingStockMap = limitUpService.getLimitUpTradeDateMap(null, null);
        redisClient.set(RedisKeyConstants.DATA_LIMIT_UP_TRADING_DATE_MAPPING_STOCK_MAP, JSON.toJSONString(limitUpMappingStockMap));
        log.info("LimitCache_initLimitUpMappingStockCache_success,RedisKey={},limitUpMappingStockMap.size={}", RedisKeyConstants.DATA_LIMIT_UP_TRADING_DATE_MAPPING_STOCK_MAP, limitUpMappingStockMap.size());
    }
}
