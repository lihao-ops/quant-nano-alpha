package com.hao.strategyengine.strategy.impl.signal;

import com.hao.strategyengine.common.model.core.StrategyContext;
import com.hao.strategyengine.common.model.response.StrategyResult;
import com.hao.strategyengine.strategy.QuantStrategy;
import enums.strategy.StrategyMetaEnum;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.*;

/**
 * 突破策略 (Breakout Strategy)
 *
 * <p><b>类职责:</b></p>
 * <p>实现一种基于价格和成交量突破的信号生成策略。当股票价格突破布林带上轨，并且成交量显著放大时，产生买入信号。</p>
 *
 * <p><b>设计目的:</b></p>
 * <ol>
 *     <li>捕捉市场进入强势上涨趋势的早期信号。</li>
 *     <li>通过结合成交量和RSI指标，过滤掉假突破和追高风险，提高信号的可靠性。</li>
 *     <li>为复合策略提供一个基础的、动量驱动的选股逻辑。</li>
 * </ol>
 *
 * <p><b>为什么需要该类:</b></p>
 * <p>在量化交易中，突破是确认趋势形成的关键信号。一个独立的突破策略类可以被灵活地组合到其他更复杂的策略中，
 * 也可以作为独立的选股池生成器，是策略库中不可或缺的基础组件。</p>
 *
 * <p><b>核心实现思路:</b></p>
 * <ol>
 *     <li><b>数据准备:</b> 从策略上下文中获取股票池和所需的历史行情数据（价格、成交量）。</li>
 *     <li><b>指标计算:</b> 对每只股票计算布林带（20日）、成交量均线（20日）和RSI（14日）等技术指标。</li>
 *     <li><b>信号筛选:</b>
 *         <ul>
 *             <li>价格突破布林带上轨 (<code>currentPrice > upperBand</code>)。</li>
 *             <li>成交量超过20日均量的1.2倍 (<code>volumeRatio > 1.2</code>)。</li>
 *             <li>RSI指标低于70，避免在超买区追高 (<code>rsi < 70</code>)。</li>
 *         </ul>
 *     </li>
 *     <li><b>信号评分与排序:</b> 对满足条件的股票，根据突破强度、成交量放大倍数和RSI健康度进行综合评分，并按分值降序排序。</li>
 *     <li><b>结果构建:</b> 封装排序后的结果，并截取前N名作为最终选股列表。</li>
 * </ol>
 *
 * @author hli
 * @date 2025-10-22
 */
@Slf4j
@Component
public class BreakoutStrategy implements QuantStrategy {

    // ================== 常量定义 ==================
    /**
     * 布林带计算周期
     */
    private static final int BOLLINGER_PERIOD = 20;
    /**
     * RSI计算周期
     */
    private static final int RSI_PERIOD = 14;
    /**
     * 布林带标准差倍数
     */
    private static final double STD_MULTIPLIER = 2.0;
    /**
     * 成交量放大阈值
     */
    private static final double VOLUME_THRESHOLD = 1.2;
    /**
     * RSI超买阈值
     */
    private static final double RSI_THRESHOLD = 70;
    /**
     * 最大返回结果数
     */
    private static final int MAX_RESULTS = 30;

    @Override
    public String getId() {
        return StrategyMetaEnum.SIG_BREAKOUT.getId();
    }

    /**
     * 执行策略的核心方法
     *
     * <p><b>实现逻辑:</b></p>
     * <ol>
     *     <li>记录策略开始时间，用于计算耗时。</li>
     *     <li>从外部依赖（如Feign客户端）获取股票池和各股票的行情数据。</li>
     *     <li>遍历股票池，对每只股票执行突破逻辑判断。</li>
     *     <li>将满足条件的股票及其信号数据存入列表。</li>
     *     <li>对列表按信号分数进行降序排序，并截取Top N。</li>
     *     <li>记录执行结果日志，并构建成功的{@link StrategyResult}。</li>
     *     <li>捕获任何异常，记录错误日志，并构建包含错误信息的{@link StrategyResult}。</li>
     * </ol>
     *
     * @param context 策略执行的上下文，包含数据源等信息
     * @return 包含选股结果和执行信息的策略结果对象
     */
    @Override
    public StrategyResult execute(StrategyContext context) {
        long start = System.currentTimeMillis();
        log.info("开始执行突破策略|Execute_breakout_strategy_start");

        try {
            // 从上下文中获取股票池
            List<String> stockPool = getStockPool(context);
            List<Map<String, Object>> selectedStocks = new ArrayList<>();

            // 遍历股票池，筛选符合条件的股票
            for (String stockCode : stockPool) {
                // TODO: 后续应从StrategyContext中获取真实数据源，并调用获取数据
                StockData stockData = getStockData(stockCode);
                
                // 数据长度不足，无法计算指标，跳过
                if (stockData.prices.size() < BOLLINGER_PERIOD) {
                    log.warn("股票数据不足_跳过计算|Stock_data_insufficient_skip_calculation,stockCode={},dataSize={}", stockCode, stockData.prices.size());
                    continue;
                }

                // 计算技术指标
                double currentPrice = stockData.prices.get(stockData.prices.size() - 1);
                double currentVolume = stockData.volumes.get(stockData.volumes.size() - 1);
                
                BollingerBands bands = calculateBollingerBands(stockData.prices);
                double volumeRatio = currentVolume / calculateAverageVolume(stockData.volumes);
                double rsi = calculateRSI(stockData.prices, RSI_PERIOD);

                // 核心筛选条件：突破、放量、未超买
                boolean isBreakout = currentPrice > bands.upperBand;
                boolean isVolumeConfirmed = volumeRatio > VOLUME_THRESHOLD;
                boolean isNotOverbought = rsi < RSI_THRESHOLD;

                if (isBreakout && isVolumeConfirmed && isNotOverbought) {
                    // 计算综合得分
                    double breakoutScore = calculateBreakoutScore(currentPrice, bands, volumeRatio, rsi);
                    
                    // 构建信号结果
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
            
            // 截取Top N结果
            if (selectedStocks.size() > MAX_RESULTS) {
                selectedStocks = selectedStocks.subList(0, MAX_RESULTS);
            }

            log.info("突破策略执行完成|Breakout_strategy_execution_finished,selectedCount={},candidateCount={}",
                    selectedStocks.size(), stockPool.size());

            // 构建成功结果
            return StrategyResult.builder()
                    .strategyId(getId())
                    .data(selectedStocks)
                    .durationMs(System.currentTimeMillis() - start)
                    .build();

        } catch (Exception e) {
            log.error("突破策略执行失败|Breakout_strategy_execution_failed", e);
            return buildErrorResult(start, e.getMessage());
        }
    }

    /**
     * 获取股票池
     * @param context 策略上下文
     * @return 股票代码列表
     */
    private List<String> getStockPool(StrategyContext context) {
        // TODO: 此处应通过 context 中的数据源客户端获取真实的股票池，例如从 quant-stock-list 服务获取
        return Arrays.asList(
            "600000.SH", "600016.SH", "600028.SH", "600030.SH", "600036.SH",
            "600048.SH", "600050.SH", "600104.SH", "600196.SH", "600276.SH",
            "600309.SH", "600519.SH", "600547.SH", "600570.SH", "600585.SH"
        );
    }

    /**
     * 获取单个股票的行情数据
     * @param stockCode 股票代码
     * @return 股票数据对象
     */
    private StockData getStockData(String stockCode) {
        // TODO: 此处应通过 context 中的数据源客户端获取真实的行情数据，例如从 quant-data-collector 服务获取
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

    /**
     * 计算布林带
     * @param prices 价格列表
     * @return 布林带对象
     */
    private BollingerBands calculateBollingerBands(List<Double> prices) {
        double ma = calculateMA(prices, BOLLINGER_PERIOD);
        double stdDev = calculateStandardDeviation(prices, ma, BOLLINGER_PERIOD);
        
        BollingerBands bands = new BollingerBands();
        bands.middleBand = ma;
        bands.upperBand = ma + (stdDev * STD_MULTIPLIER);
        bands.lowerBand = ma - (stdDev * STD_MULTIPLIER);
        return bands;
    }

    /**
     * 计算移动平均线 (MA)
     * @param prices 价格列表
     * @param period 周期
     * @return MA值
     */
    private double calculateMA(List<Double> prices, int period) {
        double sum = 0;
        int startIndex = prices.size() - period;
        for (int i = startIndex; i < prices.size(); i++) {
            sum += prices.get(i);
        }
        return sum / period;
    }

    /**
     * 计算标准差
     * @param prices 价格列表
     * @param mean 均值
     * @param period 周期
     * @return 标准差值
     */
    private double calculateStandardDeviation(List<Double> prices, double mean, int period) {
        double sumSquaredDiff = 0;
        int startIndex = prices.size() - period;
        for (int i = startIndex; i < prices.size(); i++) {
            double diff = prices.get(i) - mean;
            sumSquaredDiff += diff * diff;
        }
        return Math.sqrt(sumSquaredDiff / period);
    }

    /**
     * 计算平均成交量
     * @param volumes 成交量列表
     * @return 平均成交量
     */
    private double calculateAverageVolume(List<Double> volumes) {
        double sum = 0;
        int startIndex = Math.max(0, volumes.size() - BOLLINGER_PERIOD);
        for (int i = startIndex; i < volumes.size(); i++) {
            sum += volumes.get(i);
        }
        return sum / (volumes.size() - startIndex);
    }

    /**
     * 计算相对强弱指数 (RSI)
     * @param prices 价格列表
     * @param period 周期
     * @return RSI值
     */
    private double calculateRSI(List<Double> prices, int period) {
        // 简化版RSI计算，仅用于演示
        if (prices.size() <= period) return 50.0;
        
        double gains = 0, losses = 0;
        for (int i = prices.size() - period; i < prices.size(); i++) {
            double change = prices.get(i) - prices.get(i - 1);
            if (change > 0) {
                gains += change;
            } else {
                losses -= change;
            }
        }
        
        double avgGain = gains / period;
        double avgLoss = losses / period;
        
        if (avgLoss == 0) return 100.0;
        double rs = avgGain / avgLoss;
        return 100 - (100 / (1 + rs));
    }

    /**
     * 计算突破得分
     * @param currentPrice 当前价格
     * @param bands 布林带
     * @param volumeRatio 成交量比率
     * @param rsi RSI值
     * @return 综合得分
     */
    private double calculateBreakoutScore(double currentPrice, BollingerBands bands, 
                                        double volumeRatio, double rsi) {
        // 实现思路：综合评分 = 突破强度 + 成交量配合度 + RSI健康度
        // 1. 突破强度：价格超出上轨的百分比
        double breakoutStrength = (currentPrice - bands.upperBand) / bands.upperBand * 100;
        // 2. 成交量得分：成交量放大倍数，上限为1
        double volumeScore = Math.min(volumeRatio / 2.0, 1.0);
        // 3. RSI得分：RSI越低，说明离超买区越远，得分越高
        double rsiScore = 1.0 - (rsi / 100.0);
        
        // 加权求和
        return breakoutStrength * 0.5 + volumeScore * 0.3 + rsiScore * 0.2;
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
     * 内部数据类，用于存储从数据源获取的股票行情数据。
     * 仅在此策略内部使用，故定义为私有静态内部类。
     */
    private static class StockData {
        List<Double> prices = new ArrayList<>();
        List<Double> volumes = new ArrayList<>();
    }

    /**
     * 内部数据类，用于存储计算出的布林带上、中、下轨。
     * 仅在此策略内部使用，故定义为私有静态内部类。
     */
    private static class BollingerBands {
        double upperBand;
        double lowerBand;
        double middleBand;
    }
}
