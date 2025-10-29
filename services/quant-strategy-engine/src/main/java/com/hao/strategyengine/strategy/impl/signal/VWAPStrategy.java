package com.hao.strategyengine.strategy.impl.signal;

import com.hao.strategyengine.common.model.core.StrategyContext;
import com.hao.strategyengine.common.model.response.StrategyResult;
import com.hao.strategyengine.strategy.QuantStrategy;
import enums.strategy.StrategyMetaEnum;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.*;

/**
 * VWAPStrategy
 *
 * <p>成交量加权平均价格策略实现思路：</p>
 * <ul>
 *     <li>基于VWAP指标识别机构资金动向</li>
 *     <li>价格在VWAP之上表明买方力量强劲</li>
 *     <li>价格在VWAP之下表明卖方力量占优</li>
 *     <li>结合价格与VWAP的偏离程度判断交易机会</li>
 *     <li>使用成交量分布确认VWAP信号的有效性</li>
 * </ul>
 *
 * <p>选股逻辑：</p>
 * <ol>
 *     <li>计算当日VWAP指标</li>
 *     <li>筛选价格显著高于VWAP的股票（机构买入）</li>
 *     <li>要求成交量分布均匀，避免操纵嫌疑</li>
 *     <li>结合价格趋势确认突破有效性</li>
 *     <li>按价格-VWAP偏离程度和成交量配合度评分</li>
 * </ol>
 *
 * @author hli
 * @date 2025-10-22
 */
@Slf4j
@Component
public class VWAPStrategy implements QuantStrategy {

    private static final double PRICE_VWAP_RATIO_THRESHOLD = 1.02; // 价格高于VWAP 2%
    private static final int MIN_DATA_POINTS = 30;
    private static final int MAX_RESULTS = 25;

    @Override
    public String getId() {
        return StrategyMetaEnum.SIG_VWAP.getId();
    }

    /**
     * 执行VWAP策略选股
     *
     * @param context 策略上下文
     * @return StrategyResult 包含VWAP强势信号的股票列表
     */
    @Override
    public StrategyResult execute(StrategyContext context) {
        long start = System.currentTimeMillis();

        try {
            List<String> stockPool = getStockPool();
            List<Map<String, Object>> selectedStocks = new ArrayList<>();

            for (String stockCode : stockPool) {
                // 模拟获取分时数据和成交量
                List<PriceVolumeData> minuteData = getMinutePriceVolumeData(stockCode);

                if (minuteData.size() < MIN_DATA_POINTS) {
                    continue;
                }

                // 计算VWAP
                double vwap = calculateVWAP(minuteData);
                //todo
//                double currentPrice = minuteData.get(minuteData.size() - 1).price;
                double currentPrice = 0.00;
                double priceVwapRatio = currentPrice / vwap;

                // 计算成交量集中度（避免成交量过于集中）
                double volumeConcentration = calculateVolumeConcentration(minuteData);
                double priceTrend = calculateIntradayPriceTrend(minuteData);

                // VWAP选股条件
                boolean isPriceAboveVWAP = priceVwapRatio > PRICE_VWAP_RATIO_THRESHOLD;
                boolean isVolumeDistributed = volumeConcentration < 0.3; // 成交量集中度低于30%
                boolean isUptrend = priceTrend > 0.005; // 日内上涨超过0.5%

                if (isPriceAboveVWAP && isVolumeDistributed && isUptrend) {
                    double vwapScore = calculateVWAPScore(priceVwapRatio, volumeConcentration, priceTrend);

                    Map<String, Object> stockSignal = new HashMap<>();
                    stockSignal.put("wind_code", stockCode);
                    stockSignal.put("signal_score", vwapScore);
                    stockSignal.put("current_price", currentPrice);
                    stockSignal.put("vwap", vwap);
                    stockSignal.put("price_vwap_ratio", priceVwapRatio);
                    stockSignal.put("price_vwap_deviation", (priceVwapRatio - 1) * 100);
                    stockSignal.put("volume_concentration", volumeConcentration);
                    stockSignal.put("intraday_trend", priceTrend * 100);
                    selectedStocks.add(stockSignal);
                }
            }

            // 按VWAP信号分数降序排列
            selectedStocks.sort((a, b) ->
                    Double.compare((Double) b.get("signal_score"), (Double) a.get("signal_score")));

            if (selectedStocks.size() > MAX_RESULTS) {
                selectedStocks = selectedStocks.subList(0, MAX_RESULTS);
            }

            log.info("VWAP Strategy selected {} stocks from {} candidates",
                    selectedStocks.size(), stockPool.size());

            return StrategyResult.builder()
                    .strategyId(getId())
                    .data(selectedStocks)
                    .durationMs(System.currentTimeMillis() - start)
                    .build();

        } catch (Exception e) {
            log.error("VWAP strategy execution failed", e);
            return buildErrorResult(start, e.getMessage());
        }
    }

    /**
     * 计算VWAP（成交量加权平均价格）
     *
     * @param data 分时价格成交量数据
     * @return double VWAP值
     */
    private double calculateVWAP(List<PriceVolumeData> data) {
        double cumulativeTypicalPriceVolume = 0;
        double cumulativeVolume = 0;

        for (PriceVolumeData item : data) {
            double typicalPrice = (item.high + item.low + item.close) / 3;
            cumulativeTypicalPriceVolume += typicalPrice * item.volume;
            cumulativeVolume += item.volume;
        }

        if (cumulativeVolume == 0) {
            return 0.0;
        }

        return cumulativeTypicalPriceVolume / cumulativeVolume;
    }

    /**
     * 计算成交量集中度
     *
     * @param data 分时数据
     * @return double 成交量集中度（0-1之间，越小分布越均匀）
     */
    private double calculateVolumeConcentration(List<PriceVolumeData> data) {
        if (data.isEmpty()) {
            return 0.0;
        }

        // 计算总成交量
        double totalVolume = data.stream().mapToDouble(d -> d.volume).sum();

        // 按成交量排序，计算前20%时间段的成交量占比
        List<PriceVolumeData> sortedData = new ArrayList<>(data);
        sortedData.sort((a, b) -> Double.compare(b.volume, a.volume));

        int topCount = Math.max(1, sortedData.size() / 5); // 前20%
        double topVolume = sortedData.subList(0, topCount).stream()
                .mapToDouble(d -> d.volume).sum();

        return topVolume / totalVolume;
    }

    /**
     * 计算日内价格趋势
     *
     * @param data 分时数据
     * @return double 价格趋势（收益率）
     */
    private double calculateIntradayPriceTrend(List<PriceVolumeData> data) {
        if (data.size() < 2) {
            return 0.0;
        }

        double startPrice = data.get(0).close;
        double endPrice = data.get(data.size() - 1).close;

        return (endPrice - startPrice) / startPrice;
    }

    /**
     * 计算VWAP综合得分
     *
     * @param priceVwapRatio      价格/VWAP比率
     * @param volumeConcentration 成交量集中度
     * @param priceTrend          价格趋势
     * @return double 综合信号分数
     */
    private double calculateVWAPScore(double priceVwapRatio, double volumeConcentration, double priceTrend) {
        // 价格-VWAP偏离得分（偏离越大得分越高）
        double deviationScore = (priceVwapRatio - 1) * 100;

        // 成交量分布得分（集中度越低得分越高）
        double distributionScore = (1 - volumeConcentration) * 2;

        // 价格趋势得分
        double trendScore = priceTrend * 100;

        // 综合评分权重：偏离度50%，分布30%，趋势20%
        return deviationScore * 0.5 + distributionScore * 0.3 + trendScore * 0.2;
    }

    private List<String> getStockPool() {
        return Arrays.asList(
                "000001.SZ", "000002.SZ", "000063.SZ", "000333.SZ", "000338.SZ",
                "000651.SZ", "000725.SZ", "000768.SZ", "000858.SZ", "000876.SZ",
                "600000.SH", "600015.SH", "600016.SH", "600028.SH", "600030.SH",
                "600036.SH", "600048.SH", "600104.SH", "600276.SH", "600519.SH"
        );
    }

    /**
     * 模拟获取分时价格成交量数据
     */
    private List<PriceVolumeData> getMinutePriceVolumeData(String stockCode) {
        List<PriceVolumeData> data = new ArrayList<>();
        Random random = new Random();
        double basePrice = 15.0 + random.nextDouble() * 85;

        // 模拟一天的分时数据（240分钟）
        for (int i = 0; i < 240; i++) {
            PriceVolumeData item = new PriceVolumeData();
            double variation = (random.nextDouble() - 0.5) * 0.02; // ±1%波动

            item.open = basePrice * (1 + variation);
            item.close = basePrice * (1 + variation + (random.nextDouble() - 0.5) * 0.01);
            item.high = Math.max(item.open, item.close) * (1 + random.nextDouble() * 0.005);
            item.low = Math.min(item.open, item.close) * (1 - random.nextDouble() * 0.005);
            item.volume = 100000 + random.nextDouble() * 900000; // 10万-100万成交量

            data.add(item);

            // 轻微趋势
            basePrice *= (1 + (random.nextDouble() - 0.48) * 0.001);
        }

        return data;
    }

    private StrategyResult buildErrorResult(long start, String errorMsg) {
        Map<String, Object> errorData = new HashMap<>();
        errorData.put("error", errorMsg);
        return StrategyResult.builder()
                .strategyId(getId())
                .data(Collections.singletonList(errorData))
                .durationMs(System.currentTimeMillis() - start)
                .build();
    }

    /**
     * 分时数据内部类
     */
    private static class PriceVolumeData {
        double open;
        double high;
        double low;
        double close;
        double volume;
    }
}