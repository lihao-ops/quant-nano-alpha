package com.hao.strategyengine.strategy.impl.signal;

import com.hao.strategyengine.common.model.core.StrategyContext;
import com.hao.strategyengine.common.model.response.StrategyResult;
import com.hao.strategyengine.strategy.QuantStrategy;
import enums.strategy.StrategyMetaEnum;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.*;

/**
 * VWAP策略 (Volume-Weighted Average Price Strategy)
 *
 * <p><b>类职责:</b></p>
 * <p>实现一种基于VWAP（成交量加权平均价格）的日内交易信号策略。VWAP被认为是当天市场参与者的平均持仓成本，
 * 价格与VWAP的关系可以反映出市场的多空力量对比。</p>
 *
 * <p><b>设计目的:</b></p>
 * <ol>
 *     <li>识别出由机构资金主导的、价格持续运行在VWAP上方的强势股票。</li>
 *     <li>通过分析成交量分布，排除因少数大单导致VWAP失真的情况，确保信号的可靠性。</li>
 *     <li>结合日内价格趋势，确认买方力量的持续性，避免在冲高回落时入场。</li>
 * </ol>
 *
 * <p><b>为什么需要该类:</b></p>
 * <p>VWAP是日内交易中最重要的指标之一，尤其受到机构投资者的重视。一个独立的VWAP策略可以作为日内择时或选股的重要工具，
 * 也可以为T+0或高频交易提供决策依据。</p>
 *
 * <p><b>核心实现思路:</b></p>
 * <ol>
 *     <li><b>数据准备:</b> 获取股票池及各股票的当日分钟级行情数据（OHLC价格和成交量）。</li>
 *     <li><b>指标计算:</b>
 *         <ul>
 *             <li>计算当日至今的VWAP值。</li>
 *             <li>计算当前价格与VWAP的比率，衡量偏离程度。</li>
 *             <li>计算成交量集中度，评估成交量分布的均匀性。</li>
 *             <li>计算日内价格趋势，判断整体走势。</li>
 *         </ul>
 *     </li>
 *     <li><b>信号筛选:</b> 筛选同时满足以下条件的股票：
 *         <ul>
 *             <li><b>价格强势:</b> 当前价格显著高于VWAP（如超过2%）。</li>
 *             <li><b>成交均匀:</b> 成交量集中度较低（如低于30%），表明上涨是市场合力行为。</li>
 *             <li><b>趋势向上:</b> 日内整体价格趋势为正。</li>
 *         </ul>
 *     </li>
 *     <li><b>信号评分与排序:</b> 根据价格偏离度、成交量分布健康度和趋势强度进行加权评分。</li>
 *     <li><b>结果构建:</b> 对满足条件的股票按评分降序排序，并截取前N名。</li>
 * </ol>
 *
 * @author hli
 * @date 2025-10-22
 */
@Slf4j
@Component
public class VWAPStrategy implements QuantStrategy {

    // ================== 常量定义 ==================
    private static final double PRICE_VWAP_RATIO_THRESHOLD = 1.02; // 价格高于VWAP的阈值
    private static final double VOLUME_CONCENTRATION_THRESHOLD = 0.3; // 成交量集中度阈值
    private static final double INTRADAY_TREND_THRESHOLD = 0.005; // 日内上涨趋势阈值
    private static final int MIN_DATA_POINTS = 30; // 至少需要30分钟的数据
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
        log.info("开始执行VWAP策略|Execute_vwap_strategy_start");

        try {
            // TODO: 从context获取真实股票池
            List<String> stockPool = getStockPool();
            List<Map<String, Object>> selectedStocks = new ArrayList<>();

            for (String stockCode : stockPool) {
                // TODO: 从context获取真实分钟级行情数据
                List<PriceVolumeData> minuteData = getMinutePriceVolumeData(stockCode);

                if (minuteData.size() < MIN_DATA_POINTS) {
                    log.warn("分钟数据不足_跳过计算|Minute_data_insufficient_skip_calculation,stockCode={},dataSize={}", stockCode, minuteData.size());
                    continue;
                }

                // 计算VWAP
                double vwap = calculateVWAP(minuteData);
                if (vwap < 1e-6) {
                    log.warn("VWAP计算结果为零_跳过|VWAP_is_zero_skip,stockCode={}", stockCode);
                    continue;
                }
                
                double currentPrice = minuteData.get(minuteData.size() - 1).close;
                double priceVwapRatio = currentPrice / vwap;

                // 计算成交量集中度（避免成交量过于集中）
                double volumeConcentration = calculateVolumeConcentration(minuteData);
                double priceTrend = calculateIntradayPriceTrend(minuteData);

                // VWAP选股条件
                boolean isPriceAboveVWAP = priceVwapRatio > PRICE_VWAP_RATIO_THRESHOLD;
                boolean isVolumeDistributed = volumeConcentration < VOLUME_CONCENTRATION_THRESHOLD;
                boolean isUptrend = priceTrend > INTRADAY_TREND_THRESHOLD;

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

            log.info("VWAP策略执行完成|Vwap_strategy_execution_finished,selectedCount={},candidateCount={}",
                    selectedStocks.size(), stockPool.size());

            return StrategyResult.builder()
                    .strategyId(getId())
                    .data(selectedStocks)
                    .durationMs(System.currentTimeMillis() - start)
                    .build();

        } catch (Exception e) {
            log.error("VWAP策略执行失败|Vwap_strategy_execution_failed", e);
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

        if (cumulativeVolume < 1e-6) {
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
        if (totalVolume < 1e-6) {
            return 0.0;
        }

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

        if (startPrice < 1e-6) {
            return 0.0;
        }

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

    /**
     * 获取股票池
     * @return 股票代码列表
     */
    private List<String> getStockPool() {
        // TODO: 模拟股票池 - 实际应从数据源获取
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
        // TODO: 模拟数据 - 实际应从数据源获取
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
