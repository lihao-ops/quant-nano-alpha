package com.hao.strategyengine.strategy.impl.signal;

import com.hao.strategyengine.common.model.core.StrategyContext;
import com.hao.strategyengine.common.model.response.StrategyResult;
import com.hao.strategyengine.strategy.QuantStrategy;
import enums.strategy.StrategyMetaEnum;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.*;

/**
 * 动量策略 (Momentum Strategy)
 *
 * <p><b>类职责:</b></p>
 * <p>实现一种基于价格动量的信号生成策略。该策略旨在筛选出近期表现强势，且上涨动能仍在持续的股票。</p>
 *
 * <p><b>设计目的:</b></p>
 * <ol>
 *     <li>捕捉处于上升趋势中的股票，即“强者恒强”的投资机会。</li>
 *     <li>通过比较不同时间周期的收益率，确保动量的连续性和稳定性，避免捕捉到短暂的脉冲式上涨。</li>
 *     <li>结合成交量指标，确认动量背后有资金支持，增加信号的有效性。</li>
 * </ol>
 *
 * <p><b>为什么需要该类:</b></p>
 * <p>动量效应是市场上经过长期验证的有效因子之一。将动量逻辑封装成一个独立的策略类，
 * 不仅可以单独作为选股工具，还可以与其他因子（如价值、质量）结合，构建多因子模型，是量化策略库中的核心组成部分。</p>
 *
 * <p><b>核心实现思路:</b></p>
 * <ol>
 *     <li><b>数据准备:</b> 获取股票池及各股票近期的历史价格和成交量数据。</li>
 *     <li><b>指标计算:</b> 对每只股票计算：
 *         <ul>
 *             <li>短期（5日）、中期（10日）、长期（20日）的区间收益率。</li>
 *             <li>近期（5日）平均成交量与历史（20日）平均成交量的比率。</li>
 *         </ul>
 *     </li>
 *     <li><b>信号筛选:</b> 筛选同时满足以下条件的股票：
 *         <ul>
 *             <li><b>动量延续性:</b> 短期收益率 > 中期收益率 > 长期收益率。</li>
 *             <li><b>正向动量:</b> 所有周期的收益率均为正。</li>
 *             <li><b>成交量确认:</b> 近期成交量显著高于历史均量（如超过1.1倍）。</li>
 *         </ul>
 *     </li>
 *     <li><b>信号评分与排序:</b> 根据各周期收益率和成交量比率进行加权评分，得分越高代表动量越强。</li>
 *     <li><b>结果构建:</b> 对满足条件的股票按动量分值降序排序，并截取前N名作为最终结果。</li>
 * </ol>
 *
 * @author hli
 * @date 2025-10-22
 */
@Slf4j
@Component
public class MomentumStrategy implements QuantStrategy {

    // ================== 常量定义 ==================
    private static final int SHORT_TERM_PERIOD = 5;
    private static final int MID_TERM_PERIOD = 10;
    private static final int LONG_TERM_PERIOD = 20;
    private static final double VOLUME_RATIO_THRESHOLD = 1.1;
    private static final int MIN_DATA_SIZE = LONG_TERM_PERIOD + 1;
    private static final int MAX_RESULTS = 40;

    @Override
    public String getId() {
        return StrategyMetaEnum.SIG_MOMENTUM.getId();
    }

    /**
     * 执行动量策略
     *
     * <p><b>实现逻辑:</b></p>
     * <ol>
     *     <li>获取股票池及所需数据。</li>
     *     <li>遍历股票，计算各周期收益率和成交量比率。</li>
     *     <li>应用动量和成交量条件进行筛选。</li>
     *     <li>为满足条件的股票计算综合动量得分。</li>
     *     <li>对结果按分值排序并截取。</li>
     *     <li>记录日志并构建返回结果。</li>
     * </ol>
     *
     * @param context 策略执行上下文
     * @return 策略执行结果
     */
    @Override
    public StrategyResult execute(StrategyContext context) {
        long start = System.currentTimeMillis();
        log.info("开始执行动量策略|Execute_momentum_strategy_start");

        try {
            // TODO: 从context获取真实股票池
            List<String> stockPool = getStockPool();
            List<Map<String, Object>> selectedStocks = new ArrayList<>();

            for (String stockCode : stockPool) {
                // TODO: 从context获取真实行情数据
                List<Double> prices = getHistoricalPrices(stockCode);
                List<Double> volumes = getHistoricalVolumes(stockCode);

                if (prices.size() < MIN_DATA_SIZE) {
                    log.warn("数据量不足_跳过计算|Data_insufficient_skip_calculation,stockCode={},dataSize={}", stockCode, prices.size());
                    continue;
                }

                // 计算不同周期收益率
                double return5D = calculateReturn(prices, SHORT_TERM_PERIOD);
                double return10D = calculateReturn(prices, MID_TERM_PERIOD);
                double return20D = calculateReturn(prices, LONG_TERM_PERIOD);

                // 计算成交量比率
                double volumeRatio = calculateVolumeRatio(volumes, SHORT_TERM_PERIOD, LONG_TERM_PERIOD);

                // 动量条件：短期动量强于中长期，且均为正收益
                boolean momentumCondition = return5D > return10D && return10D > return20D && return20D > 0;
                boolean volumeCondition = volumeRatio > VOLUME_RATIO_THRESHOLD;

                if (momentumCondition && volumeCondition) {
                    double momentumScore = calculateMomentumScore(return5D, return10D, return20D, volumeRatio);

                    Map<String, Object> stockSignal = new HashMap<>();
                    stockSignal.put("wind_code", stockCode);
                    stockSignal.put("signal_score", momentumScore);
                    stockSignal.put("current_price", prices.get(prices.size() - 1));
                    stockSignal.put("return_5d", return5D * 100);
                    stockSignal.put("return_10d", return10D * 100);
                    stockSignal.put("return_20d", return20D * 100);
                    stockSignal.put("volume_ratio", volumeRatio);
                    selectedStocks.add(stockSignal);
                }
            }

            // 按动量分数降序排列
            selectedStocks.sort((a, b) ->
                    Double.compare((Double)b.get("signal_score"), (Double)a.get("signal_score")));

            if (selectedStocks.size() > MAX_RESULTS) {
                selectedStocks = selectedStocks.subList(0, MAX_RESULTS);
            }

            log.info("动量策略执行完成|Momentum_strategy_execution_finished,selectedCount={},candidateCount={}",
                    selectedStocks.size(), stockPool.size());

            return StrategyResult.builder()
                    .strategyId(getId())
                    .data(selectedStocks)
                    .durationMs(System.currentTimeMillis() - start)
                    .build();

        } catch (Exception e) {
            log.error("动量策略执行失败|Momentum_strategy_execution_failed", e);
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
                "600000.SH", "600016.SH", "600028.SH", "600030.SH", "600036.SH"
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
        double trend = 0.005;
        for (int i = 0; i < 30; i++) {
            double price = basePrice * (1 + trend * i) + random.nextDouble() * 4 - 2;
            prices.add(price);
        }
        return prices;
    }

    /**
     * 获取单个股票的历史成交量数据
     * @param stockCode 股票代码
     * @return 成交量列表
     */
    private List<Double> getHistoricalVolumes(String stockCode) {
        // TODO: 此处应通过 context 中的数据源客户端获取真实的行情数据
        List<Double> volumes = new ArrayList<>();
        Random random = new Random();
        double baseVolume = 1000000.0;
        for (int i = 0; i < 30; i++) {
            volumes.add(baseVolume * (0.8 + random.nextDouble() * 0.8));
        }
        return volumes;
    }

    /**
     * 计算指定周期内的收益率
     * @param prices 价格列表
     * @param days 周期天数
     * @return 收益率
     */
    private double calculateReturn(List<Double> prices, int days) {
        int currentIndex = prices.size() - 1;
        int pastIndex = prices.size() - 1 - days;
        if (pastIndex < 0) {
            return 0.0; // 数据不足，返回0
        }

        double currentPrice = prices.get(currentIndex);
        double pastPrice = prices.get(pastIndex);
        return (currentPrice - pastPrice) / pastPrice;
    }

    /**
     * 计算成交量比率（近期均量 / 历史均量）
     * @param volumes 成交量列表
     * @param recentDays 近期周期
     * @param historicalDays 历史周期
     * @return 成交量比率
     */
    private double calculateVolumeRatio(List<Double> volumes, int recentDays, int historicalDays) {
        double sumRecent = 0;
        double sumHistorical = 0;

        int recentStart = volumes.size() - recentDays;
        int historicalStart = volumes.size() - historicalDays;

        for (int i = recentStart; i < volumes.size(); i++) {
            sumRecent += volumes.get(i);
        }

        for (int i = historicalStart; i < recentStart; i++) {
            sumHistorical += volumes.get(i);
        }

        double avgRecent = sumRecent / recentDays;
        double avgHistorical = sumHistorical / (historicalDays - recentDays);

        if (avgHistorical < 1e-6) {
            return 0.0; // 避免除零
        }
        return avgRecent / avgHistorical;
    }

    /**
     * 计算综合动量得分
     * @param return5D 5日收益率
     * @param return10D 10日收益率
     * @param return20D 20日收益率
     * @param volumeRatio 成交量比率
     * @return 综合动量得分
     */
    private double calculateMomentumScore(double return5D, double return10D, double return20D, double volumeRatio) {
        // 实现思路：综合动量评分 = 加权收益率 + 成交量奖励
        double shortTermWeight = 0.5;
        double midTermWeight = 0.3;
        double longTermWeight = 0.1;
        double volumeWeight = 0.1;

        return return5D * 100 * shortTermWeight +
                return10D * 100 * midTermWeight +
                return20D * 100 * longTermWeight +
                volumeRatio * volumeWeight;
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
