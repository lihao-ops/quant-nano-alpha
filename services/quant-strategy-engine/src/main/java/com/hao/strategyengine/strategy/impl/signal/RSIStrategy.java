package com.hao.strategyengine.strategy.impl.signal;

import com.hao.strategyengine.common.model.core.StrategyContext;
import com.hao.strategyengine.common.model.response.StrategyResult;
import com.hao.strategyengine.strategy.QuantStrategy;
import enums.strategy.StrategyMetaEnum;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.*;

/**
 * 相对强弱指数策略 (RSI Strategy)
 *
 * <p><b>类职责:</b></p>
 * <p>实现一种基于RSI（Relative Strength Index）指标的均值回归策略。该策略旨在捕捉市场超卖后的反弹机会。</p>
 *
 * <p><b>设计目的:</b></p>
 * <ol>
 *     <li>识别市场情绪极度悲观（超卖）的时刻，这通常是价格反转的潜在信号。</li>
 *     <li>结合价格趋势和成交量变化，过滤掉单纯因下跌导致的低RSI信号（即“钝化”现象），提高抄底成功率。</li>
 *     <li>为投资组合提供一种逆向交易的信号源，与顺势策略形成对冲。</li>
 * </ol>
 *
 * <p><b>为什么需要该类:</b></p>
 * <p>RSI是最常用的震荡指标之一。在震荡市或趋势回调阶段，RSI策略能有效捕捉短线交易机会。
 * 将其封装为独立策略，可以方便地调整参数（如周期、阈值）以适应不同的市场环境。</p>
 *
 * <p><b>核心实现思路:</b></p>
 * <ol>
 *     <li><b>数据准备:</b> 获取股票池及各股票的历史价格和成交量数据。</li>
 *     <li><b>指标计算:</b>
 *         <ul>
 *             <li>计算14日RSI指标。</li>
 *             <li>计算短期（如5日）的价格趋势和成交量趋势。</li>
 *         </ul>
 *     </li>
 *     <li><b>信号筛选:</b> 筛选同时满足以下条件的股票：
 *         <ul>
 *             <li><b>超卖状态:</b> RSI < 30。</li>
 *             <li><b>价格企稳:</b> 价格趋势平稳或微涨，避免接“飞刀”。</li>
 *             <li><b>资金流入:</b> 成交量呈放大趋势。</li>
 *         </ul>
 *     </li>
 *     <li><b>信号评分与排序:</b> 根据RSI超卖程度、价格企稳情况和成交量配合度进行加权评分。</li>
 *     <li><b>结果构建:</b> 对满足条件的股票按评分降序排序，并截取前N名。</li>
 * </ol>
 *
 * @author hli
 * @date 2025-10-22
 */
@Slf4j
@Component
public class RSIStrategy implements QuantStrategy {

    // ================== 常量定义 ==================
    private static final int RSI_PERIOD = 14;
    private static final int TREND_PERIOD = 5;
    private static final double OVERSOLD_THRESHOLD = 30.0;
    private static final double OVERBOUGHT_THRESHOLD = 70.0;
    private static final double PRICE_STABLE_THRESHOLD = 0.02;
    private static final int MIN_DATA_SIZE = RSI_PERIOD + 10;
    private static final int MAX_RESULTS = 35;

    @Override
    public String getId() {
        return StrategyMetaEnum.SIG_RSI.getId();
    }

    /**
     * 执行RSI策略选股
     *
     * <p><b>实现逻辑:</b></p>
     * <ol>
     *     <li>获取股票池及所需数据。</li>
     *     <li>遍历股票，计算RSI、价格趋势和成交量趋势。</li>
     *     <li>应用超卖、企稳和放量条件进行筛选。</li>
     *     <li>为满足条件的股票计算综合得分。</li>
     *     <li>对结果按分值排序并截取。</li>
     *     <li>记录日志并构建返回结果。</li>
     * </ol>
     *
     * @param context 策略上下文，包含用户ID、股票代码等参数
     * @return StrategyResult 包含选中的股票列表，按RSI信号强度排序
     */
    @Override
    public StrategyResult execute(StrategyContext context) {
        long start = System.currentTimeMillis();
        log.info("开始执行RSI策略|Execute_rsi_strategy_start");

        try {
            // TODO: 从context获取真实股票池
            List<String> stockPool = getStockPool();
            List<Map<String, Object>> selectedStocks = new ArrayList<>();

            for (String stockCode : stockPool) {
                // TODO: 从context获取真实行情数据
                StockData stockData = getStockData(stockCode);
                
                if (stockData.prices.size() < MIN_DATA_SIZE) {
                    log.warn("数据量不足_跳过计算|Data_insufficient_skip_calculation,stockCode={},dataSize={}", stockCode, stockData.prices.size());
                    continue; 
                }

                // 计算RSI指标
                double rsi = calculateRSI(stockData.prices, RSI_PERIOD);
                double priceTrend = calculatePriceTrend(stockData.prices, TREND_PERIOD);
                double volumeTrend = calculateVolumeTrend(stockData.volumes, TREND_PERIOD);

                // RSI超卖条件判断
                boolean isOversold = rsi < OVERSOLD_THRESHOLD;
                boolean isPriceStable = Math.abs(priceTrend) < PRICE_STABLE_THRESHOLD; // 价格波动小于2%
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

            log.info("RSI策略执行完成|Rsi_strategy_execution_finished,selectedCount={},candidateCount={}",
                    selectedStocks.size(), stockPool.size());

            return StrategyResult.builder()
                    .strategyId(getId())
                    .data(selectedStocks)
                    .durationMs(System.currentTimeMillis() - start)
                    .build();

        } catch (Exception e) {
            log.error("RSI策略执行失败|Rsi_strategy_execution_failed", e);
            return buildErrorResult(start, e.getMessage());
        }
    }

    /**
     * 获取策略适用的股票池
     * 
     * @return List<String> 股票代码列表
     */
    private List<String> getStockPool() {
        // TODO: 模拟股票池 - 实际应从数据库或配置文件获取
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
        // TODO: 模拟数据 - 实际应从数据源获取
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
        errorData.put("error_message", errorMsg);
        return StrategyResult.builder()
                .strategyId(getId())
                .data(Collections.singletonList(errorData))
                .isSuccess(false)
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
