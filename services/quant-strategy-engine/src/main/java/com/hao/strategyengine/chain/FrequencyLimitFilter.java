package com.hao.strategyengine.chain;

import com.hao.strategyengine.model.Signal;
import com.hao.strategyengine.core.StrategyContext;
import lombok.extern.slf4j.Slf4j;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;

/**
 * 交易频率限制过滤器
 */
@Slf4j
public class FrequencyLimitFilter extends AbstractRiskFilter {

    private Integer maxTradesPerDay;     // 每日最大交易次数
    private Integer cooldownSeconds;      // 冷却时间（秒）

    private Map<String, Integer> dailyTradeCount = new HashMap<>();
    private Map<String, LocalDateTime> lastTradeTime = new HashMap<>();

    public FrequencyLimitFilter(Integer maxTradesPerDay, Integer cooldownSeconds) {
        this.maxTradesPerDay = maxTradesPerDay;
        this.cooldownSeconds = cooldownSeconds;
    }

    @Override
    public FilterResult filter(StrategyContext context, Signal signal) {
        log.debug("执行交易频率限制过滤器");

        String symbol = signal.getSymbol();
        LocalDateTime now = LocalDateTime.now();

        // 检查冷却时间
        LocalDateTime lastTime = lastTradeTime.get(symbol);
        if (lastTime != null) {
            long secondsSinceLastTrade = java.time.Duration.between(lastTime, now).getSeconds();
            if (secondsSinceLastTrade < cooldownSeconds) {
                String reason = String.format("冷却时间未到: 距上次交易%d秒，要求%d秒",
                        secondsSinceLastTrade, cooldownSeconds);
                log.warn(reason);
                return FilterResult.reject("FREQUENCY_LIMIT", reason);
            }
        }

        // 检查每日交易次数
        Integer count = dailyTradeCount.getOrDefault(symbol, 0);
        if (count >= maxTradesPerDay) {
            String reason = String.format("超过每日交易次数限制: 已交易%d次，限制%d次",
                    count, maxTradesPerDay);
            log.warn(reason);
            return FilterResult.reject("FREQUENCY_LIMIT", reason);
        }

        // 更新记录
        lastTradeTime.put(symbol, now);
        dailyTradeCount.put(symbol, count + 1);

        log.debug("交易频率检查通过，今日第{}次交易", count + 1);
        return passToNext(context, signal);
    }

    /**
     * 重置每日计数（定时任务调用）
     */
    public void resetDailyCount() {
        dailyTradeCount.clear();
        log.info("重置每日交易次数统计");
    }
}