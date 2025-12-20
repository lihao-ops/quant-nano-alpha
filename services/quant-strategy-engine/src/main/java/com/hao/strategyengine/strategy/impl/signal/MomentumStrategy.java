package com.hao.strategyengine.strategy.impl.signal;

import com.hao.strategyengine.common.model.core.StrategyContext;
import com.hao.strategyengine.common.model.response.StrategyResult;
import com.hao.strategyengine.strategy.QuantStrategy;
import enums.strategy.StrategyMetaEnum;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.*;

/**
 * MomentumStrategy
 *
 * <p>动量策略实现思路：</p>
 * <ul>
 *     <li>筛选近期涨幅较大且动量持续的股票</li>
 *     <li>使用不同时间周期的收益率计算综合动量得分</li>
 *     <li>结合成交量确认动量有效性</li>
 *     <li>避免选择已经过度上涨的股票，控制回撤风险</li>
 * </ul>
 *
 * <p>选股逻辑：</p>
 * <ol>
 *     <li>计算5日、10日、20日收益率</li>
 *     <li>要求短期收益率 > 中期收益率 > 长期收益率（动量延续）</li>
 *     <li>近期成交量超过均量，确认资金关注</li>
 *     <li>综合评分排序，选择动量最强的股票</li>
 * </ol>
 *
 * @author hli
 * @date 2025-10-22
 */
@Slf4j
@Component
public class MomentumStrategy implements QuantStrategy {

    private static final int MAX_RESULTS = 40;

    @Override
    public String getId() {
        return StrategyMetaEnum.SIG_MOMENTUM.getId();
    }

    @Override
    public StrategyResult execute(StrategyContext context) {
        long start = System.currentTimeMillis();

        try {
            List<String> stockPool = getStockPool();
            List<Map<String, Object>> selectedStocks = new ArrayList<>();

            for (String stockCode : stockPool) {
                List<Double> prices = getHistoricalPrices(stockCode);
                List<Double> volumes = getHistoricalVolumes(stockCode);

                if (prices.size() < 21) { // 需要至少21个交易日数据
                    continue;
                }

                // 计算不同周期收益率
                double currentPrice = prices.get(prices.size() - 1);
                double return5D = calculateReturn(prices, 5);
                double return10D = calculateReturn(prices, 10);
                double return20D = calculateReturn(prices, 20);

                // 计算成交量比率
                double volumeRatio = calculateVolumeRatio(volumes, 5);

                // 动量条件：短期动量强于中长期，且均为正收益
                boolean momentumCondition = return5D > return10D && return10D > return20D && return5D > 0;
                boolean volumeCondition = volumeRatio > 1.1;

                if (momentumCondition && volumeCondition) {
                    double momentumScore = calculateMomentumScore(return5D, return10D, return20D, volumeRatio);

                    Map<String, Object> stockSignal = new HashMap<>();
                    stockSignal.put("wind_code", stockCode);
                    stockSignal.put("signal_score", momentumScore);
                    stockSignal.put("current_price", currentPrice);
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

            log.info("日志记录|Log_message,Momentum_Strategy_selected_{}_stocks_from_{}_candidates",
                    selectedStocks.size(), stockPool.size());

            return StrategyResult.builder()
                    .strategyId(getId())
                    .data(selectedStocks)
                    .durationMs(System.currentTimeMillis() - start)
                    .build();

        } catch (Exception e) {
            log.error("日志记录|Log_message,Momentum_strategy_execution_failed", e);
            return buildErrorResult(start, e.getMessage());
        }
    }

    private List<String> getStockPool() {
        // 模拟股票池
        return Arrays.asList(
                "000001.SZ", "000002.SZ", "000063.SZ", "000069.SZ", "000100.SZ",
                "000157.SZ", "000166.SZ", "000333.SZ", "000338.SZ", "000402.SZ",
                "600000.SH", "600016.SH", "600028.SH", "600030.SH", "600036.SH"
        );
    }

    private List<Double> getHistoricalPrices(String stockCode) {
        List<Double> prices = new ArrayList<>();
        Random random = new Random();
        double basePrice = 10.0 + random.nextDouble() * 90;
        // 模拟上涨趋势
        double trend = 0.005; // 每日0.5%的上涨趋势
        for (int i = 0; i < 30; i++) {
            double price = basePrice * (1 + trend * i) + random.nextDouble() * 4 - 2;
            prices.add(price);
        }
        return prices;
    }

    private List<Double> getHistoricalVolumes(String stockCode) {
        List<Double> volumes = new ArrayList<>();
        Random random = new Random();
        double baseVolume = 1000000.0;
        for (int i = 0; i < 30; i++) {
            volumes.add(baseVolume * (0.8 + random.nextDouble() * 0.8));
        }
        return volumes;
    }

    private double calculateReturn(List<Double> prices, int days) {
        int currentIndex = prices.size() - 1;
        int pastIndex = prices.size() - 1 - days;
        if (pastIndex < 0) pastIndex = 0;

        double currentPrice = prices.get(currentIndex);
        double pastPrice = prices.get(pastIndex);
        return (currentPrice - pastPrice) / pastPrice;
    }

    private double calculateVolumeRatio(List<Double> volumes, int days) {
        double sumRecent = 0;
        double sumHistorical = 0;

        int recentStart = volumes.size() - days;
        int historicalStart = volumes.size() - 20; // 20日历史均量

        for (int i = recentStart; i < volumes.size(); i++) {
            sumRecent += volumes.get(i);
        }

        for (int i = historicalStart; i < recentStart; i++) {
            sumHistorical += volumes.get(i);
        }

        double avgRecent = sumRecent / days;
        double avgHistorical = sumHistorical / (recentStart - historicalStart);

        return avgRecent / avgHistorical;
    }

    private double calculateMomentumScore(double return5D, double return10D, double return20D, double volumeRatio) {
        // 综合动量评分：短期收益权重最高，考虑动量延续性和成交量配合
        double shortTermWeight = 0.5;
        double midTermWeight = 0.3;
        double longTermWeight = 0.1;
        double volumeWeight = 0.1;

        return return5D * 100 * shortTermWeight +
                return10D * 100 * midTermWeight +
                return20D * 100 * longTermWeight +
                volumeRatio * volumeWeight;
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
