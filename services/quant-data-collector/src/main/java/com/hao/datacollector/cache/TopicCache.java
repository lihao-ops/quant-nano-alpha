package com.hao.datacollector.cache;

import com.alibaba.fastjson.JSON;
import com.hao.datacollector.dal.dao.TopicMapper;
import com.hao.datacollector.dto.table.topic.TopicStockDTO;
import com.hao.datacollector.integration.redis.RedisClient;
import constants.RedisKeyConstants;
import jakarta.annotation.PostConstruct;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.*;

/**
 * 题材相关缓存
 *
 * 设计目的：
 * 1. 缓存题材与股票映射，提升题材查询性能。
 * 2. 统一题材缓存入口，降低数据库查询压力。
 *
 * 为什么需要该类：
 * - 题材信息常用于策略筛选，需要高效读取。
 *
 * 核心实现思路：
 * - 启动时加载题材与股票映射并写入Redis。
 *
 * @author hli
 * @program: datacollector
 * @Date 2025-08-03 22:09:00
 * @description: 题材相关缓存
 */
@Slf4j
@Component("TopicCache")
public class TopicCache {
    @Autowired
    private TopicMapper topicMapper;

    @Autowired
    private RedisClient<String> redisClient;
    /**
     * 题材信息缓存Map
     * key:题材id
     * value:题材所属类别下股票代码列表
     * <p>
     * 思路:
     * 1.先查询tb_topic_info表中所有的题材id List
     * 2.遍历查询所有
     */
    public static Map<Integer, Set<String>> topicMappingStockMap = new HashMap<>();

    /**
     * 初始化题材与股票映射缓存
     *
     * 实现逻辑：
     * 1. 查询题材与股票关联列表。
     * 2. 构建映射并写入Redis。
     */
    @PostConstruct
    public void initTopicMappingStockCache() {
        // 实现思路：
        // 1. 构建题材ID到股票集合的映射。
        // 2. 写入缓存并记录统计。
        List<TopicStockDTO> kplTopicAndStockList = topicMapper.getKplTopicAndStockList(null);
        Map<Integer, Set<String>> resultMap = new HashMap<>();
        for (TopicStockDTO dto : kplTopicAndStockList) {
            Integer topicId = dto.getTopicId();
            resultMap
                    .computeIfAbsent(topicId, k -> new HashSet<>())
                    .add(dto.getWindCode());
        }
        topicMappingStockMap = resultMap;
        redisClient.set(RedisKeyConstants.DATA_TOPIC_MAPPING_STOCK_MAP, JSON.toJSONString(topicMappingStockMap));
        log.info("题材缓存完成|Topic_cache_loaded,topicStockSize={}", kplTopicAndStockList.size());
    }
}
