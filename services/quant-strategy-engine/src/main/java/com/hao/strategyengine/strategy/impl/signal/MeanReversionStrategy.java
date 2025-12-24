package com.hao.strategyengine.strategy.impl.signal;

import com.hao.strategyengine.common.model.core.StrategyContext;
import com.hao.strategyengine.common.model.response.StrategyResult;
import com.hao.strategyengine.strategy.QuantStrategy;
import enums.strategy.StrategyMetaEnum;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.*;

/**
 * 均值回归策略 (Mean Reversion Strategy)
 *
 * <p><b>类职责:</b></p>
 * <p>实现一种基于统计套利的均值回归策略。该策略假设股票价格会围绕其历史均值波动，当价格过度偏离均值时，有向均值回归的倾向。</p>
 *
 * <p><b>设计目的:</b></p>
 * <ol>
 *     <li>识别出当前价格远低于其历史均值的股票，作为潜在的买入机会。</li>
 *     <li>通过Z-score量化价格偏离程度，提供一个标准化的、可比较的抄底信号强度。</li>
 *     <li>作为一种反向投资策略，与趋势跟踪策略（如动量策略）形成互补。</li>
 * </ol>
 *
 * <p><b>为什么需要该类:</b></p>
 * <p>均值回归是金融市场中一个经典的统计现象。将此逻辑封装成一个独立的策略类，可以方便地用于构建多策略投资组合，
 * 特别是在震荡市场中，此类策略往往能表现出良好的效果，是策略库中重要的组成部分。</p>
 *
 * <p><b>核心实现思路:</b></p>
 * <ol>
 *     <li><b>数据准备:</b> 获取股票池中所有股票的近期历史价格数据。</li>
 *     <li><b>指标计算:</b> 对每只股票：
 *         <ul>
 *             <li>计算N日（如20日）的移动平均价 (MA)。</li>
 *             <li>计算N日的价格标准差 (Standard Deviation)。</li>
 *             <li>计算Z-score = (当前价格 - MA) / 标准差。</li>
 *         </ul>
 *     </li>
 *     <li><b>信号筛选:</b> 筛选出Z-score低于某一负阈值（如-1.0）的股票，这表示股价已显著低于其近期均值。</li>
 *     <li><b>信号评分与排序:</b> Z-score越低，表示价格被低估的可能性越大，回归的潜在空间也越大。因此，将-Z-score作为信号分数，并按此分数降序排列。</li>
 *     <li><b>结果构建:</b> 封装排序后的结果，并截取前N名作为最终的抄底备选池。</li>
 * </ol>
 *
 * @author hli
 * @date 2025-10-22
 */
@Slf4j
@Component
public class MeanReversionStrategy implements QuantStrategy {

    // ================== 常量定义 ==================
    /**
     * 计算均值和标准差的回看周期
     */
    private static final int LOOKBACK_PERIOD = 20;
    /**
     * Z-score筛选阈值，低于此值的股票被认为是潜在的买入信号
     */
    private static final double ZSCORE_THRESHOLD = -1.0;
    /**
     * 最大返回结果数
     */
    private static final int MAX_RESULTS = 50;

    @Override
    public String getId() {
        return StrategyMetaEnum.SIG_MEAN_REVERSION.getId();
    }

    /**
     * 执行均值回归策略
     *
     * <p><b>实现逻辑:</b></p>
     * <ol>
     *     <li>获取股票池和相应的历史价格数据。</li>
     *     <li>遍历每只股票，计算其Z-score。</li>
     *     <li>如果Z-score低于预设阈值，则认为出现交易信号。</li>
     *     <li>将信号分数（-Z-score）和其他相关指标存入结果列表。</li>
     *     <li>对结果列表按信号分数降序排序，并截取Top N。</li>
     *     <li>记录日志并构建包含最终结果的{@link StrategyResult}。</li>
     *     <li>在发生异常时，记录错误并返回失败的{@link StrategyResult}。</li>
     * </ol>
     *
     * @param context 策略执行上下文
     * @return 策略执行结果
     */
    @Override
    public StrategyResult execute(StrategyContext context) {
        long start = System.currentTimeMillis();
        log.info("开始执行均值回归策略|Execute_mean_reversion_strategy_start");

        try {
            // TODO: 后续应从 context 获取真实的股票池
            List<String> stockPool = getStockPool();
            List<Map<String, Object>> selectedStocks = new ArrayList<>();

            for (String stockCode : stockPool) {
                // TODO: 后续应从 context 获取真实的行情数据
                List<Double> historicalPrices = getHistoricalPrices(stockCode);
                
                if (historicalPrices.size() < LOOKBACK_PERIOD) {
                    log.warn("股票数据不足_跳过计算|Stock_data_insufficient_skip_calculation,stockCode={},dataSize={}", stockCode, historicalPrices.size());
                    continue;
                }

                // 计算核心指标
                double currentPrice = historicalPrices.get(historicalPrices.size() - 1);
                double movingAverage = calculateMovingAverage(historicalPrices);
                double standardDeviation = calculateStandardDeviation(historicalPrices, movingAverage);
                
                // Z-score为0或标准差过小可能导致除零异常或结果无意义
                if (standardDeviation < 1e-6) {
                    log.warn("价格波动过小_跳过计算|Price_fluctuation_too_small_skip_calculation,stockCode={}", stockCode);
                    continue;
                }
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

            log.info("均值回归策略执行完成|Mean_reversion_strategy_execution_finished,selectedCount={},candidateCount={}",
                    selectedStocks.size(), stockPool.size());

            return StrategyResult.builder()
                    .strategyId(getId())
                    .data(selectedStocks)
                    .durationMs(System.currentTimeMillis() - start)
                    .build();

        } catch (Exception e) {
            log.error("均值回归策略执行失败|Mean_reversion_strategy_execution_failed", e);
            return buildErrorResult(start, e.getMessage());
        }
    }

    /**
     * 获取股票池
     * @return 股票代码列表
     */
    private List<String> getStockPool() {
        // TODO: 此处应通过 context 中的数据源客户端获取真实的股票池
        return Arrays.asList(
            "000001.SZ", "000002.SZ", "000063.SZ", "000069.SZ", "000100.SZ",
            "000157.SZ", "000166.SZ", "000333.SZ", "000338.SZ", "000402.SZ",
            "000413.SZ", "000415.SZ", "000423.SZ", "000425.SZ", "000538.SZ"
        );
    }

    /**
     * 获取单个股票的历史价格数据
     * @param stockCode 股票代码
     * @return 价格列表
     */
    private List<Double> getHistoricalPrices(String stockCode) {
        // TODO: 此处应通过 context 中的数据源客户端获取真实的行情数据
        List<Double> prices = new ArrayList<>();
        Random random = new Random();
        double basePrice = 10.0 + random.nextDouble() * 90;
        for (int i = 0; i < 30; i++) {
            prices.add(basePrice + random.nextDouble() * 20 - 10);
        }
        return prices;
    }

    /**
     * 计算移动平均值
     * @param prices 价格列表
     * @return 移动平均值
     */
    private double calculateMovingAverage(List<Double> prices) {
        double sum = 0;
        int startIndex = prices.size() - LOOKBACK_PERIOD;
        for (int i = startIndex; i < prices.size(); i++) {
            sum += prices.get(i);
        }
        return sum / LOOKBACK_PERIOD;
    }

    /**
     * 计算标准差
     * @param prices 价格列表
     * @param mean 均值
     * @return 标准差
     */
    private double calculateStandardDeviation(List<Double> prices, double mean) {
        double sumSquaredDiff = 0;
        int startIndex = prices.size() - LOOKBACK_PERIOD;
        for (int i = startIndex; i < prices.size(); i++) {
            double diff = prices.get(i) - mean;
            sumSquaredDiff += diff * diff;
        }
        return Math.sqrt(sumSquaredDiff / LOOKBACK_PERIOD);
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
