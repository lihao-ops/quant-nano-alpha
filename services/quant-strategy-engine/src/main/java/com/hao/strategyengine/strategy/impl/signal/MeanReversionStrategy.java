package com.hao.strategyengine.strategy.impl.signal;

import com.hao.strategyengine.common.model.core.StrategyContext;
import com.hao.strategyengine.common.model.response.StrategyResult;
import com.hao.strategyengine.strategy.QuantStrategy;
import enums.strategy.StrategyMetaEnum;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.*;

/**
 * MeanReversionStrategy
 *
 * <p>均值回归策略实现思路：</p>
 * <ul>
 *     <li>基于价格偏离移动平均线的程度进行选股</li>
 *     <li>计算每只股票的Z-score = (当前价格 - N日MA) / N日标准差</li>
 *     <li>筛选Z-score显著为负的股票（价格低于均值），Z-score越小买入信号越强</li>
 *     <li>返回按Z-score排序的股票列表，Z-score最小的股票最有可能均值回归</li>
 * </ul>
 *
 * <p>选股逻辑：</p>
 * <ol>
 *     <li>获取股票池中所有股票的历史价格数据</li>
 *     <li>计算每只股票的20日移动平均线和标准差</li>
 *     <li>计算Z-score衡量价格偏离程度</li>
 *     <li>筛选Z-score < -1.0的股票，按Z-score升序排列</li>
 *     <li>返回前N只最有可能均值回归的股票</li>
 * </ol>
 *
 * @author hli
 * @date 2025-10-22
 */
@Slf4j
@Component
public class MeanReversionStrategy implements QuantStrategy {

    private static final int LOOKBACK_PERIOD = 20;
    private static final double ZSCORE_THRESHOLD = -1.0;
    private static final int MAX_RESULTS = 50;

    @Override
    public String getId() {
        return StrategyMetaEnum.SIG_MEAN_REVERSION.getId();
    }

    @Override
    public StrategyResult execute(StrategyContext context) {
        long start = System.currentTimeMillis();

        try {
            // 模拟获取股票池（实际应从配置或参数获取）
            List<String> stockPool = getStockPool();
            List<Map<String, Object>> selectedStocks = new ArrayList<>();

            for (String stockCode : stockPool) {
                // 模拟获取股票历史数据
                List<Double> historicalPrices = getHistoricalPrices(stockCode);
                
                if (historicalPrices.size() < LOOKBACK_PERIOD) {
                    continue;
                }

                double currentPrice = historicalPrices.get(historicalPrices.size() - 1);
                double movingAverage = calculateMovingAverage(historicalPrices);
                double standardDeviation = calculateStandardDeviation(historicalPrices, movingAverage);
                double zScore = (currentPrice - movingAverage) / standardDeviation;

                // 筛选符合条件的股票
                if (zScore < ZSCORE_THRESHOLD) {
                    Map<String, Object> stockSignal = new HashMap<>();
                    stockSignal.put("wind_code", stockCode);
                    stockSignal.put("signal_score", -zScore); // 负的Z-score转为正分数，越大越好
                    stockSignal.put("z_score", zScore);
                    stockSignal.put("current_price", currentPrice);
                    stockSignal.put("moving_average", movingAverage);
                    stockSignal.put("deviation_percent", (currentPrice - movingAverage) / movingAverage * 100);
                    selectedStocks.add(stockSignal);
                }
            }

            // 按信号分数降序排列（Z-score越小，信号分数越大）
            selectedStocks.sort((a, b) -> 
                Double.compare((Double)b.get("signal_score"), (Double)a.get("signal_score")));
            
            // 限制返回数量
            if (selectedStocks.size() > MAX_RESULTS) {
                selectedStocks = selectedStocks.subList(0, MAX_RESULTS);
            }

            log.info("MeanReversion Strategy selected {} stocks from {} candidates", 
                    selectedStocks.size(), stockPool.size());

            return StrategyResult.builder()
                    .strategyId(getId())
                    .data(selectedStocks)
                    .durationMs(System.currentTimeMillis() - start)
                    .build();

        } catch (Exception e) {
            log.error("MeanReversion strategy execution failed", e);
            return buildErrorResult(start, e.getMessage());
        }
    }

    private List<String> getStockPool() {
        // 模拟股票池 - 实际应从数据库或配置获取
        return Arrays.asList(
            "000001.SZ", "000002.SZ", "000063.SZ", "000069.SZ", "000100.SZ",
            "000157.SZ", "000166.SZ", "000333.SZ", "000338.SZ", "000402.SZ",
            "000413.SZ", "000415.SZ", "000423.SZ", "000425.SZ", "000538.SZ"
        );
    }

    private List<Double> getHistoricalPrices(String stockCode) {
        // 模拟数据 - 实际应从 tb_quotation_index_history_trend 查询
        List<Double> prices = new ArrayList<>();
        Random random = new Random();
        double basePrice = 10.0 + random.nextDouble() * 90; // 10-100元股价
        for (int i = 0; i < 30; i++) {
            prices.add(basePrice + random.nextDouble() * 20 - 10);
        }
        return prices;
    }

    private double calculateMovingAverage(List<Double> prices) {
        double sum = 0;
        int startIndex = prices.size() - LOOKBACK_PERIOD;
        for (int i = startIndex; i < prices.size(); i++) {
            sum += prices.get(i);
        }
        return sum / LOOKBACK_PERIOD;
    }

    private double calculateStandardDeviation(List<Double> prices, double mean) {
        double sumSquaredDiff = 0;
        int startIndex = prices.size() - LOOKBACK_PERIOD;
        for (int i = startIndex; i < prices.size(); i++) {
            double diff = prices.get(i) - mean;
            sumSquaredDiff += diff * diff;
        }
        return Math.sqrt(sumSquaredDiff / LOOKBACK_PERIOD);
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
}