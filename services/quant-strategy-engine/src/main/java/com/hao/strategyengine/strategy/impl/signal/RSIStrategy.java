package com.hao.strategyengine.strategy.impl.signal;

import com.hao.strategyengine.common.model.core.StrategyContext;
import com.hao.strategyengine.common.model.response.StrategyResult;
import com.hao.strategyengine.strategy.QuantStrategy;
import enums.strategy.StrategyMetaEnum;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.*;

/**
 * RSIStrategy
 *
 * <p>相对强弱指数策略实现思路：</p>
 * <ul>
 *     <li>基于RSI指标识别超买超卖状态进行选股</li>
 *     <li>RSI低于30为超卖区域，可能产生买入机会</li>
 *     <li>RSI高于70为超买区域，可能产生卖出信号</li>
 *     <li>结合价格趋势和成交量确认RSI信号的有效性</li>
 *     <li>使用RSI背离现象增强策略效果</li>
 * </ul>
 *
 * <p>选股逻辑：</p>
 * <ol>
 *     <li>计算14日RSI指标</li>
 *     <li>筛选RSI处于超卖区域(RSI < 30)的股票</li>
 *     <li>要求价格处于上升趋势或底部震荡状态</li>
 *     <li>成交量逐步放大，确认资金流入</li>
 *     <li>按RSI超卖程度和价格位置综合评分</li>
 * </ol>
 *
 * @author hli
 * @date 2025-10-22
 */
@Slf4j
@Component
public class RSIStrategy implements QuantStrategy {

    private static final int RSI_PERIOD = 14;
    private static final double OVERSOLD_THRESHOLD = 30.0;
    private static final double OVERBOUGHT_THRESHOLD = 70.0;
    private static final int MAX_RESULTS = 35;

    @Override
    public String getId() {
        return StrategyMetaEnum.SIG_RSI.getId();
    }

    /**
     * 执行RSI策略选股
     * 
     * @param context 策略上下文，包含用户ID、股票代码等参数
     * @return StrategyResult 包含选中的股票列表，按RSI信号强度排序
     */
    @Override
    public StrategyResult execute(StrategyContext context) {
        long start = System.currentTimeMillis();

        try {
            List<String> stockPool = getStockPool();
            List<Map<String, Object>> selectedStocks = new ArrayList<>();

            for (String stockCode : stockPool) {
                StockData stockData = getStockData(stockCode);
                
                if (stockData.prices.size() < RSI_PERIOD + 10) {
                    continue; // 数据不足
                }

                // 计算RSI指标
                double rsi = calculateRSI(stockData.prices, RSI_PERIOD);
                double priceTrend = calculatePriceTrend(stockData.prices, 5);
                double volumeTrend = calculateVolumeTrend(stockData.volumes, 5);

                // RSI超卖条件判断
                boolean isOversold = rsi < OVERSOLD_THRESHOLD;
                boolean isPriceStable = Math.abs(priceTrend) < 0.02; // 价格波动小于2%
                boolean isVolumeIncreasing = volumeTrend > 0;

                if (isOversold && (isPriceStable || priceTrend > 0) && isVolumeIncreasing) {
                    double rsiScore = calculateRSIScore(rsi, priceTrend, volumeTrend);
                    
                    Map<String, Object> stockSignal = new HashMap<>();
                    stockSignal.put("wind_code", stockCode);
                    stockSignal.put("signal_score", rsiScore);
                    stockSignal.put("rsi", rsi);
                    stockSignal.put("price_trend", priceTrend * 100);
                    stockSignal.put("volume_trend", volumeTrend * 100);
                    stockSignal.put("current_price", stockData.prices.get(stockData.prices.size() - 1));
                    selectedStocks.add(stockSignal);
                }
            }

            // 按RSI信号分数降序排列（RSI越低，超卖程度越高，分数越高）
            selectedStocks.sort((a, b) -> 
                Double.compare((Double)b.get("signal_score"), (Double)a.get("signal_score")));
            
            if (selectedStocks.size() > MAX_RESULTS) {
                selectedStocks = selectedStocks.subList(0, MAX_RESULTS);
            }

            log.info("日志记录|Log_message,RSI_Strategy_selected_{}_stocks_from_{}_candidates", 
                    selectedStocks.size(), stockPool.size());

            return StrategyResult.builder()
                    .strategyId(getId())
                    .data(selectedStocks)
                    .durationMs(System.currentTimeMillis() - start)
                    .build();

        } catch (Exception e) {
            log.error("日志记录|Log_message,RSI_strategy_execution_failed", e);
            return buildErrorResult(start, e.getMessage());
        }
    }

    /**
     * 获取策略适用的股票池
     * 
     * @return List<String> 股票代码列表
     */
    private List<String> getStockPool() {
        // 模拟股票池 - 实际应从数据库或配置文件获取
        return Arrays.asList(
            "000001.SZ", "000002.SZ", "000063.SZ", "000069.SZ", "000100.SZ",
            "000157.SZ", "000166.SZ", "000333.SZ", "000338.SZ", "000402.SZ",
            "000413.SZ", "000415.SZ", "000423.SZ", "000425.SZ", "000538.SZ",
            "600000.SH", "600016.SH", "600028.SH", "600030.SH", "600036.SH"
        );
    }

    /**
     * 模拟获取股票历史数据
     * 
     * @param stockCode 股票代码
     * @return StockData 包含价格和成交量数据
     */
    private StockData getStockData(String stockCode) {
        StockData data = new StockData();
        Random random = new Random();
        double basePrice = 10.0 + random.nextDouble() * 90;
        double baseVolume = 1000000.0;
        
        // 模拟价格数据，包含一定的波动性
        for (int i = 0; i < 30; i++) {
            double price = basePrice * (1 + (random.nextDouble() - 0.5) * 0.1);
            data.prices.add(price);
            data.volumes.add(baseVolume * (0.7 + random.nextDouble() * 0.6));
        }
        return data;
    }

    /**
     * 计算RSI指标
     * 
     * @param prices 价格序列
     * @param period RSI计算周期
     * @return double RSI值
     */
    private double calculateRSI(List<Double> prices, int period) {
        if (prices.size() <= period) {
            return 50.0; // 数据不足时返回中性值
        }
        
        List<Double> gains = new ArrayList<>();
        List<Double> losses = new ArrayList<>();
        
        // 计算价格变动
        for (int i = prices.size() - period; i < prices.size(); i++) {
            double change = prices.get(i) - prices.get(i - 1);
            if (change > 0) {
                gains.add(change);
                losses.add(0.0);
            } else {
                gains.add(0.0);
                losses.add(-change);
            }
        }
        
        // 计算平均增益和平均损失
        double avgGain = gains.stream().mapToDouble(Double::doubleValue).average().orElse(0.0);
        double avgLoss = losses.stream().mapToDouble(Double::doubleValue).average().orElse(0.0);
        
        if (avgLoss == 0) {
            return 100.0; // 避免除零
        }
        
        double rs = avgGain / avgLoss;
        return 100 - (100 / (1 + rs));
    }

    /**
     * 计算价格趋势
     * 
     * @param prices 价格序列
     * @param days 趋势计算天数
     * @return double 价格趋势（百分比变化）
     */
    private double calculatePriceTrend(List<Double> prices, int days) {
        if (prices.size() < days + 1) {
            return 0.0;
        }
        
        double currentPrice = prices.get(prices.size() - 1);
        double pastPrice = prices.get(prices.size() - 1 - days);
        return (currentPrice - pastPrice) / pastPrice;
    }

    /**
     * 计算成交量趋势
     * 
     * @param volumes 成交量序列
     * @param days 趋势计算天数
     * @return double 成交量趋势（百分比变化）
     */
    private double calculateVolumeTrend(List<Double> volumes, int days) {
        if (volumes.size() < days + 1) {
            return 0.0;
        }
        
        double sumRecent = 0;
        double sumPast = 0;
        
        // 计算近期成交量均值
        for (int i = volumes.size() - days; i < volumes.size(); i++) {
            sumRecent += volumes.get(i);
        }
        double avgRecent = sumRecent / days;
        
        // 计算前期成交量均值
        for (int i = volumes.size() - 2 * days; i < volumes.size() - days; i++) {
            if (i >= 0) {
                sumPast += volumes.get(i);
            }
        }
        double avgPast = sumPast / days;
        
        if (avgPast == 0) {
            return 0.0;
        }
        
        return (avgRecent - avgPast) / avgPast;
    }

    /**
     * 计算RSI综合得分
     * 
     * @param rsi RSI值
     * @param priceTrend 价格趋势
     * @param volumeTrend 成交量趋势
     * @return double 综合信号分数
     */
    private double calculateRSIScore(double rsi, double priceTrend, double volumeTrend) {
        // RSI超卖程度得分（RSI越低得分越高）
        double rsiScore = (OVERSOLD_THRESHOLD - rsi) / OVERSOLD_THRESHOLD;
        
        // 价格趋势得分（趋势向上得分高）
        double trendScore = Math.max(priceTrend * 10, 0);
        
        // 成交量趋势得分（量增得分高）
        double volumeScore = Math.max(volumeTrend * 5, 0);
        
        // 综合评分权重：RSI 60%，价格趋势 25%，成交量 15%
        return rsiScore * 0.6 + trendScore * 0.25 + volumeScore * 0.15;
    }

    /**
     * 构建错误结果
     */
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
     * 内部数据类，用于组织股票数据
     */
    private static class StockData {
        List<Double> prices = new ArrayList<>();
        List<Double> volumes = new ArrayList<>();
    }
}
