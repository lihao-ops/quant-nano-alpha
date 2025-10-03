package com.hao.strategyengine.strategies;

import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.TypeReference;
import com.hao.strategyengine.config.SpringContextHolder;
import com.hao.strategyengine.core.StrategyContext;
import com.hao.strategyengine.integration.feign.DataCollectorClient;
import com.hao.strategyengine.integration.redis.RedisClient;
import com.hao.strategyengine.model.Signal;
import com.hao.strategyengine.template.AbstractStrategy;
import constants.RedisKeyConstants;
import dto.HistoryTrendDTO;
import lombok.extern.slf4j.Slf4j;

import java.time.LocalDateTime;
import java.util.*;
import java.util.stream.Collectors;

/**
 * 龙一战法（Long One Strategy）
 * 通过 DataCollectorClient 获取远程数据源的数据进行策略计算。
 * 已实现的核心步骤：
 * 1. calculateIndicators: 调用 Feign 客户端获取交易日、题材股票映射、涨停股等数据并缓存到 StrategyContext
 * 2. generateSignal: 基于缓存的数据实现简单的龙一信号判定逻辑，返回 Signal
 * 3. initialize: 初始日志/初始化操作
 */
@Slf4j
public class LongOneStrategy extends AbstractStrategy {

    // Feign 客户端，用于远程数据查询
    private DataCollectorClient collectorClient;

    private RedisClient<String> redisClient;

    public LongOneStrategy() {
        super("LONG_ONE_STRATEGY");
    }

    private void initDependencies() {
        if (collectorClient == null || redisClient == null) {
            collectorClient = SpringContextHolder.getBean(DataCollectorClient.class);
            redisClient = SpringContextHolder.getBean(RedisClient.class);
        }
    }

    /**
     * 构造函数注入 DataCollectorClient
     *
     * @param collectorClient Feign 客户端，来自 quant-data-collector
     */
    public LongOneStrategy(DataCollectorClient collectorClient, RedisClient<String> redisClient) {
        super("LONG_ONE_STRATEGY");
        this.collectorClient = collectorClient;
        this.redisClient = redisClient;
    }

    /**
     * 计算指标/获取数据
     * 思路：
     * - 获取需要的远程数据（示例中：交易日、题材-股票映射、涨停股票等）
     * - 将数据缓存进 StrategyContext，供后续的 generateSignal 使用
     * 具体实现可根据实际数据接口的字段名称做调整
     */
    @Override
    protected void calculateIndicators(StrategyContext context) {
        //初始化依赖
        initDependencies();
        // 1. 获取交易日列表（示例：年份固定为 "2022"；实际可通过上下文传入）
        List<String> tradeDates;
        try {
            tradeDates = collectorClient.getTradeDateListByTime(
                    context.getParameters().get("startTime").toString(),
                    context.getParameters().get("endTime").toString());
        } catch (Exception e) {
            // 兜底处理，避免策略引擎因为数据请求失败而中断
            tradeDates = null;
            System.err.println("Failed to fetch trade dates: " + e.getMessage());
        }
        // 2. 获取题材对应股票集合
        Map<Integer, Set<String>> topicMappingStockMap;
        try {
            String topicMappingStockMapStr = redisClient.get(RedisKeyConstants.DATA_TOPIC_MAPPING_STOCK_MAP);
            topicMappingStockMap = JSON.parseObject(
                    topicMappingStockMapStr,
                    new TypeReference<Map<Integer, Set<String>>>() {
                    }
            );
        } catch (Exception e) {
            topicMappingStockMap = new HashMap<>();
            log.error("LongOneStrategy_Failed_to_fetch_topic_stocks:{}", e.getMessage());
        }
        // 3. 获取涨停股票（针对年份）
        //key:交易日期,value:当天涨停股票代码Set
        Map<String, Set<String>> limitUpMappingStockMap;
        try {
            String limitUpMappingStockMapStr = redisClient.get(RedisKeyConstants.DATA_LIMIT_UP_TRADING_DATE_MAPPING_STOCK_MAP);
            limitUpMappingStockMap = JSON.parseObject(
                    limitUpMappingStockMapStr,
                    new TypeReference<Map<String, Set<String>>>() {
                    }
            );
        } catch (Exception e) {
            limitUpMappingStockMap = new HashMap<>();
            System.err.println("Failed_to_fetch_limit-up_stocks: " + e.getMessage());
        }
        // 4. 将数据放入 StrategyContext，供 generateSignal 使用
        if (!tradeDates.isEmpty()) {
            context.setData("tradeDates", tradeDates);
        }
        if (!topicMappingStockMap.isEmpty()) {
            context.setData("topicMappingStockMap", topicMappingStockMap);
        }
        if (!limitUpMappingStockMap.isEmpty()) {
            context.setData("limitUpMappingStockMap", limitUpMappingStockMap);
        }
    }

    /**
     * 根据缓存数据生成交易信号
     * 简单示例策略：
     * - 遍历交易日，查找当天涨停股中属于任一题材集合的股票比例
     * - 若命中比例达到阈值（示例 0.3），则选择当天涨停中符合条件的股票作为 LONG 信号
     * - 未命中则返回 null，表示不产生信号
     */
    @Override
    protected Signal generateSignal(StrategyContext context) {
        List<Signal> result = new ArrayList<>();
        // 从 context 取出之前缓存的数据
        @SuppressWarnings("unchecked")
        List<String> tradeDates = (List<String>) context.getData("tradeDates");
        //key:交易日期,value:当天涨停股票代码Set
        @SuppressWarnings("unchecked")
        Map<String, Set<String>> topicStockMap = (Map<String, Set<String>>) context.getData("topicMappingStockMap");
        @SuppressWarnings("unchecked")
        Map<String, Set<String>> limitUpMap = (Map<String, Set<String>>) context.getData("limitUpMappingStockMap");

        if (tradeDates == null || topicStockMap == null || limitUpMap == null) {
            // 缓存数据不完整，直接返回 null
            return null;
        }
        /**
         * 龙一战法策略
         * 1. 遍历交易日列表，获取涨停股
         * 2. 遍历涨停股，判断是否属于任一题材
         * 3. 获取涨停股列表中属于任一题材的股票
         */
        for (String tradeDate : tradeDates) {
            Set<String> limitUpStockSet = limitUpMap.get(tradeDate);
            List<String> limitUpStockByDate = limitUpStockSet == null ? Collections.emptyList() : new ArrayList<>(limitUpStockSet);

            for (Set<String> topicStockSet : topicStockMap.values()) {
                List<String> topicStockList = topicStockSet == null ? Collections.emptyList() : new ArrayList<>(topicStockSet);
                // 取交集
                Set<String> intersection = new HashSet<>(limitUpStockByDate);
                intersection.retainAll(topicStockList);
                // 比例：交集 ÷ limitUpStockByDate
                double ratio = limitUpStockByDate.isEmpty() ? 0 : (double) intersection.size() / limitUpStockByDate.size();
                if (ratio >= Double.parseDouble(context.getParameters().get("topicLimitRatio").toString())) {
                    List<String> intersectionList = intersection.stream().toList();
                    //找出龙一
                    List<HistoryTrendDTO> trendDataByStockList = collectorClient.getHistoryTrendDataByStockList(tradeDate, tradeDate, intersectionList);
                    /**
                     * 龙一必须满足两个条件：
                     * 1.在当天所有板块下的涨停股票中最先涨停
                     * 2.涨停成交额最大
                     */
                    String longOneStock = findLongOne(trendDataByStockList);
                    if (longOneStock != null) {
                        Signal signal = new Signal();
                        signal.setSymbol(longOneStock);
                        signal.setReason(tradeDate);
                        signal.setType(Signal.SignalType.BUY);
                        signal.setStrategyName("LongOneStrategy");
                        signal.setTimestamp(LocalDateTime.now());
                        result.add(signal);
                    }
                }
            }
        }
        if (!result.isEmpty()) {
            Signal signal = new Signal();
            signal.setType(Signal.SignalType.CLOSE);
            signal.setStrategyName("LongOneStrategy");
            signal.setTimestamp(LocalDateTime.now());
            signal.setExtra(result);
            return signal;
        }
        // 未命中条件，返回空信号
        return null;
    }

    /**
     * 找出龙一
     *
     * @param trendData 多股票列表的当日分时数据
     * @return 符合龙一条件的股票代码
     */
    public String findLongOne(List<HistoryTrendDTO> trendData) {
        if (trendData.isEmpty()) {
            return null;
        }
        Map<String, HistoryTrendDTO> candidateMap = new HashMap<>();
        // 按股票分组
        Map<String, List<HistoryTrendDTO>> grouped = trendData.stream()
                .collect(Collectors.groupingBy(HistoryTrendDTO::getWindCode));
        for (Map.Entry<String, List<HistoryTrendDTO>> entry : grouped.entrySet()) {
            String stock = entry.getKey();
            List<HistoryTrendDTO> data = entry.getValue();
            data.sort(Comparator.comparing(HistoryTrendDTO::getTradeDate)); // 时间升序

            boolean firstLimitFound = false;
            double limitPrice = 0.0;
            for (int i = 0; i < data.size(); i++) {
                HistoryTrendDTO dto = data.get(i);
                // 找第一笔涨停
                if (!firstLimitFound) {
                    limitPrice = dto.getLatestPrice();
                    firstLimitFound = true;
                    // 检查之后是否有破价
                    double finalLimitPrice = limitPrice;
                    boolean broken = data.stream()
                            .skip(i + 1)
                            .anyMatch(d -> d.getLatestPrice() < finalLimitPrice);
                    if (!broken) {
                        // 满足“最早且全天不破该价”
                        candidateMap.put(stock, dto);
                        break; // 找到该股票符合条件的第一笔，跳出
                    }
                }
            }
        }
        // 按成交额排序
        return candidateMap.entrySet().stream()
                .max(Comparator.comparingDouble(e -> e.getValue().getTotalVolume() * e.getValue().getLatestPrice()))
                .map(Map.Entry::getKey)
                .orElse(null);
    }


    /**
     * 辅助方法：在给定日期的涨停股票列表中，挑选一个候选股票
     * 这里演示性地返回列表中的第一个元素。实际中可结合风控、趋势数据等进一步筛选。
     * 如需要更精细的数据，可以在这里调用 client.getTrendData(date, limitUpStocks) 获取趋势分时数据等信息。
     */
    private String findFirstLimitUp(List<String> limitUpStocks, String date) {
        if (limitUpStocks != null && !limitUpStocks.isEmpty()) {
            // 简单策略：直接返回列表第一个交易日的涨停股
            return limitUpStocks.get(0);
        }
        return null;
    }

    /**
     * 初始化方法（可选）
     * 如需要在策略启动阶段做一些准备工作，可以在这里实现
     */
    @Override
    public void initialize(StrategyContext context) {
        log.info("初始化龙一战法（Long One Strategy）...");
        // 如有需要，可以预加载一些固定数据、缓存开关等
        Map<String, Object> parameters = new HashMap<>();
        parameters.put("startTime", "2024-01-01");
        parameters.put("endTime", "2024-12-31");
        parameters.put("topicLimitRatio", 0.1);
        context.setParameters(parameters);
        this.ready = true;
        log.info("LONG_ONE_STRATEGY策略初始化完成");
    }
}
