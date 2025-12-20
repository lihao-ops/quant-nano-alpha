package com.hao.strategyengine.strategy.impl.information;

import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.TypeReference;
import com.hao.strategyengine.common.model.core.StrategyContext;
import com.hao.strategyengine.common.model.response.StrategyResult;
import com.hao.strategyengine.integration.redis.RedisClient;
import com.hao.strategyengine.strategy.QuantStrategy;
import constants.RedisKeyConstants;
import enums.strategy.StrategyMetaEnum;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.util.CollectionUtils;
import org.springframework.util.StringUtils;

import java.util.*;
import java.util.stream.Collectors;

/**
 * HotTopicStrategy
 * ===============================================
 * 实现思路：
 * 1. 从 Redis 获取题材与股票的映射信息，作为策略运行的基础数据。
 * 2. 根据上下文入参判断按题材 ID 或名称进行过滤，构建匹配股票列表。
 * 3. 将去重后的结果封装为 StrategyResult 返回，并记录执行耗时。
 * ===============================================
 * 【功能定位】
 * ⦿ 基于题材库的热点策略（Hot Topic Strategy）
 * ⦿ 支持按题材名称模糊搜索或题材ID精确查询
 * ⦿ 查询数据源优先使用 Redis 缓存
 * ===============================================
 * <p>
 * 【Redis 缓存数据结构】
 * Key: RedisKeyConstants.DATA_TOPIC_MAPPING_STOCK_MAP
 * Value: JSON Map<Integer topicId, Set<String> stockCodes>
 * <p>
 * 【输入参数】
 * context.getExtra().get("topicId")   → 精确查询
 * context.getExtra().get("topicName") → 模糊查询
 * <p>
 * 【输出】
 * StrategyResult.data = 匹配的股票代码集合
 *
 * @author hli
 * @date 2025-10-26
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class HotTopicStrategy implements QuantStrategy {

    private final RedisClient<String> redisClient;

    @Override
    public String getId() {
        return StrategyMetaEnum.INFO_HOT_TOPIC.getId();
    }

    @Override
    @SuppressWarnings("unchecked")
    public StrategyResult execute(StrategyContext context) {
        long start = System.currentTimeMillis();
        Map<String, Object> extra = context.getExtra();

        // Step 1⃣ 尝试从Redis读取题材映射数据
        String json = redisClient.get(RedisKeyConstants.DATA_TOPIC_MAPPING_STOCK_MAP);
        if (!StringUtils.hasText(json)) {
            log.warn("Redis中未找到题材映射缓存，topicMap为空");
            return StrategyResult.builder()
                    .strategyId(getId())
                    .data(Collections.emptyList())
                    .durationMs(System.currentTimeMillis() - start)
                    .build();
        }
        // 替换原来的 parseObject(json, Map.class)
        Map<Integer, Set<String>> topicMap = JSON.parseObject(
                json, new TypeReference<Map<Integer, Set<String>>>() {
                }
        );
        List<String> resultStocks = new ArrayList<>();
        // Step 2⃣ 根据输入参数判断查询类型
        if (extra != null) {
            Object topicIdObj = extra.get("topicId");
            //todo topicName 模糊查询暂不支持
            Object topicNameObj = extra.get("topicName");

            // 按ID查询
            if (topicIdObj != null) {
                Integer topicId = Integer.valueOf(topicIdObj.toString());
                Set<String> stocks = topicMap.get(topicId);
                if (!CollectionUtils.isEmpty(stocks)) {
                    resultStocks.addAll(stocks);
                }
                log.info("[HotTopicStrategy]_按题材ID={}_查询匹配股票数={}", topicId, resultStocks.size());
            }
            // 按名称模糊查询（这里演示匹配Redis中的key或模拟查库）
            else if (topicNameObj != null) {
                String topicName = topicNameObj.toString().toLowerCase();
                //  模拟模糊匹配逻辑（真实情况应从数据库或TopicCache模糊匹配）
                for (Map.Entry<Integer, Set<String>> entry : topicMap.entrySet()) {
                    if (String.valueOf(entry.getKey()).contains(topicName)) {
                        resultStocks.addAll(entry.getValue());
                    }
                }
                //  模拟库查询（仅当Redis无匹配时）
                if (resultStocks.isEmpty()) {
                    log.info("[HotTopicStrategy]_Redis未命中，尝试从DB/远程服务加载...");
                    // 示例：从数据库加载或RPC调用
                    // List<String> dbResult = topicMapper.queryByNameLike(topicName);
                }
                log.info("[HotTopicStrategy]_按名称模糊查询={},_匹配股票数={}", topicName, resultStocks.size());
            }
        }

        // Step 3⃣ 返回策略结果
        return StrategyResult.builder()
                .strategyId(getId())
                .data(resultStocks.stream().distinct().collect(Collectors.toList()))
                .durationMs(System.currentTimeMillis() - start)
                .build();
    }
}
