package com.hao.strategyengine.event.listener;

import com.hao.strategyengine.event.StrategyEvent;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

/**
 * 指标统计监听器
 */
@Slf4j
@Component
public class MetricsEventListener implements StrategyEventListener {

    private long signalCount = 0;
    private long orderCount = 0;
    private long rejectionCount = 0;
    private long errorCount = 0;

    @Override
    public void onEvent(StrategyEvent event) {
        switch (event.getType()) {
            case SIGNAL_GENERATED:
                signalCount++;
                break;
            case ORDER_EXECUTED:
                orderCount++;
                break;
            case RISK_REJECTED:
                rejectionCount++;
                break;
            case STRATEGY_ERROR:
                errorCount++;
                break;
        }

        // 每100个事件记录一次统计
        if ((signalCount + orderCount + rejectionCount + errorCount) % 100 == 0) {
            logMetrics();
        }
    }

    @Override
    public String getName() {
        return "MetricsEventListener";
    }

    /**
     * 记录指标
     */
    private void logMetrics() {
        log.info("策略指标统计 - 信号数:{}, 订单数:{}, 拒绝数:{}, 错误数:{}",
                signalCount, orderCount, rejectionCount, errorCount);
    }

    /**
     * 获取统计数据
     */
    public MetricsData getMetrics() {
        return new MetricsData(signalCount, orderCount, rejectionCount, errorCount);
    }

    public static class MetricsData {
        public final long signalCount;
        public final long orderCount;
        public final long rejectionCount;
        public final long errorCount;

        public MetricsData(long signalCount, long orderCount, long rejectionCount, long errorCount) {
            this.signalCount = signalCount;
            this.orderCount = orderCount;
            this.rejectionCount = rejectionCount;
            this.errorCount = errorCount;
        }
    }
}
