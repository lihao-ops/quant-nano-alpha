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
 * 涨停相关缓存
 *
 * 设计目的：
 * 1. 缓存涨停日期与股票映射，提升查询效率。
 * 2. 降低服务层对数据库的频繁访问。
 *
 * 为什么需要该类：
 * - 涨停数据在多处查询中复用，适合统一缓存管理。
 *
 * 核心实现思路：
 * - 启动时批量拉取涨停映射并写入Redis。
 *
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

    /**
     * 初始化涨停日期与股票映射缓存
     *
     * 实现逻辑：
     * 1. 从服务层批量获取涨停映射。
     * 2. 序列化后写入Redis。
     */
    @PostConstruct
    public void initLimitUpMappingStockCache() {
        // 实现思路：
        // 1. 拉取涨停映射结果。
        // 2. 写入Redis缓存。
        //key:交易日期,value:当天涨停股票代码Set
        Map<String, Set<String>> limitUpMappingStockMap = limitUpService.getLimitUpTradeDateMap(null, null);
        redisClient.set(RedisKeyConstants.DATA_LIMIT_UP_TRADING_DATE_MAPPING_STOCK_MAP, JSON.toJSONString(limitUpMappingStockMap));
        log.info("涨停映射缓存完成|Limit_up_mapping_cache_done,redisKey={},mapSize={}",
                RedisKeyConstants.DATA_LIMIT_UP_TRADING_DATE_MAPPING_STOCK_MAP, limitUpMappingStockMap.size());
    }
}
