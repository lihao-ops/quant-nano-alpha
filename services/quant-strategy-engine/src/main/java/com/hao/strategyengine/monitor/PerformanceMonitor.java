package com.hao.strategyengine.monitor;

import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.Timer;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

/**
 * 性能监控组件
 */
@Slf4j
@Component
public class PerformanceMonitor {

    private final MeterRegistry meterRegistry;

    // 计数器
    private final Counter signalGeneratedCounter;
    private final Counter orderExecutedCounter;
    private final Counter riskRejectedCounter;

    // 计时器
    private final Timer strategyAnalysisTimer;
    private final Timer orderExecutionTimer;

    public PerformanceMonitor(MeterRegistry meterRegistry) {
        this.meterRegistry = meterRegistry;

        // 初始化计数器
        this.signalGeneratedCounter = Counter.builder("strategy.signal.generated")
                .description("交易信号生成总数")
                .tag("type", "all")
                .register(meterRegistry);

        this.orderExecutedCounter = Counter.builder("strategy.order.executed")
                .description("订单执行总数")
                .tag("type", "all")
                .register(meterRegistry);

        this.riskRejectedCounter = Counter.builder("strategy.risk.rejected")
                .description("风控拒绝总数")
                .tag("type", "all")
                .register(meterRegistry);

        // 初始化计时器
        this.strategyAnalysisTimer = Timer.builder("strategy.analysis.time")
                .description("策略分析耗时")
                .register(meterRegistry);

        this.orderExecutionTimer = Timer.builder("strategy.order.execution.time")
                .description("订单执行耗时")
                .register(meterRegistry);
    }

    /**
     * 记录信号生成
     */
    public void recordSignalGenerated(String strategyName, String signalType) {
        Counter.builder("strategy.signal.generated")
                .tag("strategy", strategyName)
                .tag("signal_type", signalType)
                .register(meterRegistry)
                .increment();

        signalGeneratedCounter.increment();
    }

    /**
     * 记录订单执行
     */
    public void recordOrderExecuted(String symbol, String orderType) {
        Counter.builder("strategy.order.executed")
                .tag("symbol", symbol)
                .tag("order_type", orderType)
                .register(meterRegistry)
                .increment();

        orderExecutedCounter.increment();
    }

    /**
     * 记录风控拒绝
     */
    public void recordRiskRejected(String filterName, String reason) {
        Counter.builder("strategy.risk.rejected")
                .tag("filter", filterName)
                .tag("reason", reason)
                .register(meterRegistry)
                .increment();

        riskRejectedCounter.increment();
    }

    /**
     * 记录策略分析耗时
     */
    public void recordAnalysisTime(String strategyName, long milliseconds) {
        Timer.builder("strategy.analysis.time")
                .tag("strategy", strategyName)
                .register(meterRegistry)
                .record(java.time.Duration.ofMillis(milliseconds));
    }

    /**
     * 执行带监控的策略分析
     */
    public <T> T executeWithTiming(String strategyName,
                                   java.util.function.Supplier<T> supplier) {
        return Timer.builder("strategy.analysis.time")
                .tag("strategy", strategyName)
                .register(meterRegistry)
                .record(supplier);
    }
}
