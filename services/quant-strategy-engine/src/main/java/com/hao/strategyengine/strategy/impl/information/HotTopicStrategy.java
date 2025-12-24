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
 * 热点题材策略 (Hot Topic Strategy)
 *
 * <p><b>类职责:</b></p>
 * <p>根据指定的热点题材（概念），从预先构建的题材-股票映射关系中，筛选出相关的股票列表。</p>
 *
 * <p><b>设计目的:</b></p>
 * <ol>
 *     <li>提供一种快速筛选题材股的能力，服务于事件驱动或主题投资类的量化策略。</li>
 *     <li>将数据获取与策略逻辑解耦，策略本身不关心数据如何产生，只负责从Redis中消费。</li>
 *     <li>支持按题材ID精确查询和按题材名称模糊查询（待实现），满足不同场景的需求。</li>
 * </ol>
 *
 * <p><b>为什么需要该类:</b></p>
 * <p>题材轮动是A股市场的重要特征。一个高效的题材选股策略是捕捉市场短期热点、获取超额收益的关键工具。
 * 将其封装为独立策略，可以方便地被上层应用（如选股器、组合策略）调用。</p>
 *
 * <p><b>核心实现思路:</b></p>
 * <ol>
 *     <li><b>数据源:</b> 策略依赖于一个预置在Redis中的题材-股票映射表。
 *         <ul>
 *             <li><b>Key:</b> {@link RedisKeyConstants#DATA_TOPIC_MAPPING_STOCK_MAP}</li>
 *             <li><b>Value (JSON):</b> {@code Map<Integer, Set<String>>} (题材ID -> 股票代码集合)</li>
 *         </ul>
 *     </li>
 *     <li><b>参数解析:</b> 从 {@link StrategyContext} 的 {@code extra} 参数中获取 {@code topicId} 或 {@code topicName}。</li>
 *     <li><b>逻辑执行:</b>
 *         <ul>
 *             <li>优先根据 {@code topicId} 进行精确匹配，效率最高。</li>
 *             <li>如果 {@code topicId} 不存在，则根据 {@code topicName} 进行模糊匹配（当前为模拟逻辑，未来需对接真实数据服务）。</li>
 *         </ul>
 *     </li>
 *     <li><b>结果返回:</b> 返回去重后的股票代码列表。如果Redis缓存不存在或未匹配到任何股票，则返回空列表。</li>
 * </ol>
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

    /**
     * 执行热点题材策略
     *
     * <p><b>实现逻辑:</b></p>
     * <ol>
     *     <li>从Redis获取题材-股票映射的JSON字符串。</li>
     *     <li>如果缓存为空，记录警告并返回空结果。</li>
     *     <li>使用FastJSON将JSON解析为 {@code Map<Integer, Set<String>>}。</li>
     *     <li>根据上下文参数，分派到按ID查询或按名称查询的私有方法。</li>
     *     <li>对查询结果去重，并构建成功的策略结果。</li>
     *     <li>捕获所有异常，记录错误并返回失败的策略结果。</li>
     * </ol>
     *
     * @param context 策略执行上下文
     * @return 策略执行结果
     */
    @Override
    @SuppressWarnings("unchecked")
    public StrategyResult execute(StrategyContext context) {
        long start = System.currentTimeMillis();
        log.info("开始执行热点题材策略|Execute_hot_topic_strategy_start,context={}", context);

        try {
            // 1. 从Redis读取题材映射数据
            String json = redisClient.get(RedisKeyConstants.DATA_TOPIC_MAPPING_STOCK_MAP);
            if (!StringUtils.hasText(json)) {
                log.warn("题材映射缓存未找到|Topic_mapping_cache_not_found,key={}", RedisKeyConstants.DATA_TOPIC_MAPPING_STOCK_MAP);
                return StrategyResult.builder()
                        .strategyId(getId())
                        .data(Collections.emptyList())
                        .durationMs(System.currentTimeMillis() - start)
                        .build();
            }

            // 2. 解析JSON
            // 注意：此处使用FastJSON，项目中其他地方可能使用Jackson，需注意保持一致性。
            Map<Integer, Set<String>> topicMap = JSON.parseObject(json, new TypeReference<Map<Integer, Set<String>>>() {});
            if (CollectionUtils.isEmpty(topicMap)) {
                log.warn("题材映射数据为空|Topic_mapping_data_is_empty,key={}", RedisKeyConstants.DATA_TOPIC_MAPPING_STOCK_MAP);
                return StrategyResult.builder().strategyId(getId()).data(Collections.emptyList()).build();
            }

            // 3. 根据参数执行查询
            Set<String> resultStocks = new HashSet<>();
            Map<String, Object> extra = context.getExtra();
            if (extra != null) {
                Object topicIdObj = extra.get("topicId");
                Object topicNameObj = extra.get("topicName");

                if (topicIdObj != null) {
                    resultStocks.addAll(findByTopicId(topicIdObj, topicMap));
                } else if (topicNameObj != null) {
                    resultStocks.addAll(findByTopicName(topicNameObj, topicMap));
                }
            }

            log.info("热点题材策略执行完成|Hot_topic_strategy_execution_finished,matchCount={}", resultStocks.size());

            // 4. 返回策略结果
            return StrategyResult.builder()
                    .strategyId(getId())
                    .data(new ArrayList<>(resultStocks)) // 转换为List
                    .durationMs(System.currentTimeMillis() - start)
                    .build();
        } catch (Exception e) {
            log.error("热点题材策略执行失败|Hot_topic_strategy_execution_failed", e);
            return buildErrorResult(start, e.getMessage());
        }
    }

    /**
     * 按题材ID精确查询
     */
    private Set<String> findByTopicId(Object topicIdObj, Map<Integer, Set<String>> topicMap) {
        try {
            Integer topicId = Integer.valueOf(topicIdObj.toString());
            Set<String> stocks = topicMap.getOrDefault(topicId, Collections.emptySet());
            log.info("按题材ID查询|Find_by_topic_id,topicId={},matchCount={}", topicId, stocks.size());
            return stocks;
        } catch (NumberFormatException e) {
            log.warn("题材ID格式错误|Invalid_topic_id_format,topicIdObj={}", topicIdObj);
            return Collections.emptySet();
        }
    }

    /**
     * 按题材名称模糊查询
     */
    private Set<String> findByTopicName(Object topicNameObj, Map<Integer, Set<String>> topicMap) {
        // TODO: 当前为模拟实现。真实场景应通过更高效的方式（如数据库索引、倒排索引或专门的缓存）进行模糊查询。
        String topicName = topicNameObj.toString().toLowerCase();
        Set<String> matchedStocks = new HashSet<>();
        log.warn("执行模糊查询_性能较低|Performing_fuzzy_search_low_performance,topicName={}", topicName);

        // 模拟逻辑：遍历所有key进行匹配，性能较低，仅用于演示
        for (Map.Entry<Integer, Set<String>> entry : topicMap.entrySet()) {
            // 假设题材名称存储在另一个地方，这里用ID模拟
            if (String.valueOf(entry.getKey()).contains(topicName)) {
                matchedStocks.addAll(entry.getValue());
            }
        }
        log.info("按题材名称模糊查询|Find_by_topic_name,topicName={},matchCount={}", topicName, matchedStocks.size());
        return matchedStocks;
    }

    /**
     * 构建错误的策略结果
     * @param start 策略开始时间
     * @param errorMsg 错误信息
     * @return 包含错误信息的策略结果
     */
    private StrategyResult buildErrorResult(long start, String errorMsg) {
        Map<String, Object> errorData = new HashMap<>();
        errorData.put("error_message", errorMsg);
        return StrategyResult.builder()
                .strategyId(getId())
                .data(Collections.singletonList(errorData))
                .isSuccess(false)
                .durationMs(System.currentTimeMillis() - start)
                .build();
    }
}
