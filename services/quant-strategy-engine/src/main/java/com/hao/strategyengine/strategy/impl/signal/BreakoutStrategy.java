package com.hao.strategyengine.strategy.impl.signal;

import com.hao.strategyengine.common.model.core.StrategyContext;
import com.hao.strategyengine.common.model.response.StrategyResult;
import com.hao.strategyengine.strategy.QuantStrategy;
import enums.strategy.StrategyMetaEnum;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.*;

/**
 * BreakoutStrategy
 *
 * <p>突破策略实现思路：</p>
 * <ul>
 *     <li>筛选价格突破布林带上轨且成交量放大的股票</li>
 *     <li>结合RSI指标过滤超买信号，避免追高</li>
 *     <li>要求成交量超过平均成交量的一定比例，确认突破有效性</li>
 *     <li>返回按突破强度和成交量配合度排序的股票列表</li>
 * </ul>
 *
 * <p>选股逻辑：</p>
 * <ol>
 *     <li>计算每只股票的布林带（20日MA±2倍标准差）</li>
 *     <li>筛选当前价格突破上轨的股票</li>
 *     <li>要求成交量超过20日均量1.2倍以上</li>
 *     <li>RSI指标小于70，避免超买区域</li>
 *     <li>按突破幅度和成交量配合度综合评分排序</li>
 * </ol>
 *
 * @author hli
 * @date 2025-10-22
 */
@Slf4j
@Component
public class BreakoutStrategy implements QuantStrategy {

    private static final int BOLLINGER_PERIOD = 20;
    private static final double STD_MULTIPLIER = 2.0;
    private static final double VOLUME_THRESHOLD = 1.2;
    private static final double RSI_THRESHOLD = 70;
    private static final int MAX_RESULTS = 30;

    @Override
    public String getId() {
        return StrategyMetaEnum.SIG_BREAKOUT.getId();
    }

    @Override
    public StrategyResult execute(StrategyContext context) {
        long start = System.currentTimeMillis();

        try {
            List<String> stockPool = getStockPool();
            List<Map<String, Object>> selectedStocks = new ArrayList<>();

            for (String stockCode : stockPool) {
                StockData stockData = getStockData(stockCode);
                
                if (stockData.prices.size() < BOLLINGER_PERIOD) {
                    continue;
                }

                // 计算技术指标
                double currentPrice = stockData.prices.get(stockData.prices.size() - 1);
                double currentVolume = stockData.volumes.get(stockData.volumes.size() - 1);
                
                BollingerBands bands = calculateBollingerBands(stockData.prices);
                double volumeRatio = currentVolume / calculateAverageVolume(stockData.volumes);
                double rsi = calculateRSI(stockData.prices, 14);

                // 突破条件判断
                boolean isBreakout = currentPrice > bands.upperBand;
                boolean volumeConfirm = volumeRatio > VOLUME_THRESHOLD;
                boolean notOverbought = rsi < RSI_THRESHOLD;

                if (isBreakout && volumeConfirm && notOverbought) {
                    double breakoutScore = calculateBreakoutScore(currentPrice, bands, volumeRatio, rsi);
                    
                    Map<String, Object> stockSignal = new HashMap<>();
                    stockSignal.put("wind_code", stockCode);
                    stockSignal.put("signal_score", breakoutScore);
                    stockSignal.put("current_price", currentPrice);
                    stockSignal.put("upper_band", bands.upperBand);
                    stockSignal.put("breakout_percent", (currentPrice - bands.upperBand) / bands.upperBand * 100);
                    stockSignal.put("volume_ratio", volumeRatio);
                    stockSignal.put("rsi", rsi);
                    selectedStocks.add(stockSignal);
                }
            }

            // 按突破分数降序排列
            selectedStocks.sort((a, b) -> 
                Double.compare((Double)b.get("signal_score"), (Double)a.get("signal_score")));
            
            if (selectedStocks.size() > MAX_RESULTS) {
                selectedStocks = selectedStocks.subList(0, MAX_RESULTS);
            }

            log.info("Breakout Strategy selected {} stocks from {} candidates", 
                    selectedStocks.size(), stockPool.size());

            return StrategyResult.builder()
                    .strategyId(getId())
                    .data(selectedStocks)
                    .durationMs(System.currentTimeMillis() - start)
                    .build();

        } catch (Exception e) {
            log.error("Breakout strategy execution failed", e);
            return buildErrorResult(start, e.getMessage());
        }
    }

    private List<String> getStockPool() {
        // 模拟股票池
        return Arrays.asList(
            "600000.SH", "600016.SH", "600028.SH", "600030.SH", "600036.SH",
            "600048.SH", "600050.SH", "600104.SH", "600196.SH", "600276.SH",
            "600309.SH", "600519.SH", "600547.SH", "600570.SH", "600585.SH"
        );
    }

    private StockData getStockData(String stockCode) {
        StockData data = new StockData();
        Random random = new Random();
        double basePrice = 10.0 + random.nextDouble() * 90;
        double baseVolume = 1000000.0;
        
        for (int i = 0; i < 30; i++) {
            data.prices.add(basePrice + random.nextDouble() * 20 - 10);
            data.volumes.add(baseVolume * (0.8 + random.nextDouble() * 0.8));
        }
        return data;
    }

    private BollingerBands calculateBollingerBands(List<Double> prices) {
        double ma = calculateMA(prices, BOLLINGER_PERIOD);
        double stdDev = calculateStandardDeviation(prices, ma, BOLLINGER_PERIOD);
        
        BollingerBands bands = new BollingerBands();
        bands.middleBand = ma;
        bands.upperBand = ma + (stdDev * STD_MULTIPLIER);
        bands.lowerBand = ma - (stdDev * STD_MULTIPLIER);
        return bands;
    }

    private double calculateMA(List<Double> prices, int period) {
        double sum = 0;
        int startIndex = prices.size() - period;
        for (int i = startIndex; i < prices.size(); i++) {
            sum += prices.get(i);
        }
        return sum / period;
    }

    private double calculateStandardDeviation(List<Double> prices, double mean, int period) {
        double sumSquaredDiff = 0;
        int startIndex = prices.size() - period;
        for (int i = startIndex; i < prices.size(); i++) {
            double diff = prices.get(i) - mean;
            sumSquaredDiff += diff * diff;
        }
        return Math.sqrt(sumSquaredDiff / period);
    }

    private double calculateAverageVolume(List<Double> volumes) {
        double sum = 0;
        int startIndex = Math.max(0, volumes.size() - BOLLINGER_PERIOD);
        for (int i = startIndex; i < volumes.size(); i++) {
            sum += volumes.get(i);
        }
        return sum / (volumes.size() - startIndex);
    }

    private double calculateRSI(List<Double> prices, int period) {
        // 简化版RSI计算
        if (prices.size() <= period) return 50.0;
        
        double gains = 0, losses = 0;
        for (int i = prices.size() - period; i < prices.size(); i++) {
            double change = prices.get(i) - prices.get(i - 1);
            if (change > 0) gains += change;
            else losses -= change;
        }
        
        double avgGain = gains / period;
        double avgLoss = losses / period;
        
        if (avgLoss == 0) return 100.0;
        double rs = avgGain / avgLoss;
        return 100 - (100 / (1 + rs));
    }

    private double calculateBreakoutScore(double currentPrice, BollingerBands bands, 
                                        double volumeRatio, double rsi) {
        // 综合评分：突破幅度 + 成交量配合 + RSI健康度
        double breakoutStrength = (currentPrice - bands.upperBand) / bands.upperBand * 100;
        double volumeScore = Math.min(volumeRatio / 2.0, 1.0); // 成交量得分
        double rsiScore = 1.0 - (rsi / 100.0); // RSI越低得分越高
        
        return breakoutStrength * 0.5 + volumeScore * 0.3 + rsiScore * 0.2;
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

    // 内部数据类
    private static class StockData {
        List<Double> prices = new ArrayList<>();
        List<Double> volumes = new ArrayList<>();
    }

    private static class BollingerBands {
        double upperBand;
        double lowerBand;
        double middleBand;
    }
}