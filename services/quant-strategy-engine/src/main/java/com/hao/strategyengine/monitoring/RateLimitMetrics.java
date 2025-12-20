package com.hao.strategyengine.monitoring;

import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.Timer;
import jakarta.annotation.PostConstruct;
import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.EnableScheduling;
import org.springframework.stereotype.Component;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.TimeUnit;

/**
 * 限流监控
 * <p>
 * 核心改进:
 * 1. 预注册 Counter/Timer,避免重复注册
 * 2. 移除高基数的 user_id tag
 * 3. 安全获取 P99 延迟
 */
@Slf4j
@Component
@EnableScheduling
public class RateLimitMetrics {

    @Resource
    private MeterRegistry meterRegistry;

    // 缓存已注册的 Counter,避免重复注册
    private final Map<String, Counter> rejectCounters = new ConcurrentHashMap<>();

    // 缓存已注册的 Timer,避免重复注册
    private final Map<String, Timer> waitTimers = new ConcurrentHashMap<>();

    @PostConstruct
    public void init() {
        log.info("_限流监控指标初始化完成|Log_message");
    }

    /**
     * 记录限流拒绝事件
     * <p>
     * 改进:
     * - 移除 user_id tag (避免高基数)
     * - 使用缓存避免重复注册
     */
    public void recordRateLimitReject(String limitType, String userId, String strategyType) {
        String key = limitType + ":" + strategyType;

        Counter counter = rejectCounters.computeIfAbsent(key, k ->
                Counter.builder("rate_limit.reject.count")
                        .description("限流拒绝次数统计")
                        .tag("limit_type", limitType)
                        .tag("strategy_type", strategyType)
                        .register(meterRegistry)
        );

        counter.increment();

        // 可选: 如果需要知道具体用户,用日志而不是metric
        log.debug("限流拒绝:_limitType={},_userId={},_strategyType={}",
                limitType, hashUserId(userId), strategyType);
    }

    /**
     * 记录等待时间
     * <p>
     * 改进: 使用缓存避免重复注册
     */
    public void recordWaitTime(String limitType, long waitMillis) {
        Timer timer = waitTimers.computeIfAbsent(limitType, k ->
                Timer.builder("rate_limit.wait_time")
                        .description("限流等待耗时分布")
                        .tag("limit_type", limitType)
                        .publishPercentiles(0.5, 0.9, 0.99) // P50, P90, P99
                        .register(meterRegistry)
        );

        timer.record(waitMillis, TimeUnit.MILLISECONDS);
    }

    /**
     * 每10秒输出一次监控摘要
     * <p>
     * 改进: 安全获取 P99,遍历所有 limitType
     */
//    @Scheduled(fixedRate = 10000)
    public void reportMetrics() {
        StringBuilder report = new StringBuilder("\n========== 限流监控摘要 ==========\n");

        // 输出等待时间统计
        for (Map.Entry<String, Timer> entry : waitTimers.entrySet()) {
            String limitType = entry.getKey();
            Timer timer = entry.getValue();

            if (timer.count() > 0) {
                double meanMs = timer.mean(TimeUnit.MILLISECONDS);
                double maxMs = timer.max(TimeUnit.MILLISECONDS);

                // 安全获取 P99
                try {
                    double p99Ms = timer.takeSnapshot()
                            .percentileValues()
                            [timer.takeSnapshot().percentileValues().length - 1]
                            .value(TimeUnit.MILLISECONDS);

                    report.append(String.format(" [%s] 检查次数: %d, 平均: %.2fms, P99: %.2fms, 最大: %.2fms\n",
                            limitType, timer.count(), meanMs, p99Ms, maxMs));
                } catch (Exception e) {
                    log.warn("无法获取P99延迟:_{}", e.getMessage());
                }
            }
        }

        // 输出拒绝统计
        for (Map.Entry<String, Counter> entry : rejectCounters.entrySet()) {
            String key = entry.getKey();
            Counter counter = entry.getValue();

            if (counter.count() > 0) {
                report.append(String.format(" [%s] 拒绝次数: %.0f\n",
                        key, counter.count()));
            }
        }

        report.append("==================================");
        //todo 待完善
        String reportText = report.toString().replace(" ", "_").replace("\n", "\\n");
        if (reportText.contains("检查次数") || reportText.contains("拒绝次数")) {
            log.info("限流监控报告|Rate_limit_report,report={}", reportText);
        }
    }

    /**
     * 用户ID脱敏（hash）
     */
    private String hashUserId(String userId) {
        if (userId == null) return "unknown";
        int hash = Math.abs(userId.hashCode());
        return Integer.toHexString(hash % 10000);
    }
}

// ==================== 可选: 更完善的版本 ====================

/**
 * 如果你想要更完善的监控,可以添加这些方法
 */
/*
@Component
@Slf4j
public class RateLimitMetricsAdvanced {

    @Resource
    private MeterRegistry meterRegistry;

    // 记录限流通过 (计算通过率用)
    public void recordRateLimitPass(String limitType, String strategyType) {
        Counter.builder("rate_limit.pass.count")
                .tag("limit_type", limitType)
                .tag("strategy_type", strategyType)
                .register(meterRegistry)
                .increment();
    }

    // 记录降级事件
    public void recordFallbackToLocal(String reason) {
        Counter.builder("rate_limit.fallback.count")
                .tag("reason", reason)
                .register(meterRegistry)
                .increment();
    }

    // 计算通过率
    @Scheduled(fixedRate = 30000)
    public void reportPassRate() {
        // 遍历所有 limitType
        for (String limitType : Arrays.asList("GLOBAL", "USER", "STRATEGY_TYPE")) {
            Counter passCounter = meterRegistry.find("rate_limit.pass.count")
                .tag("limit_type", limitType)
                .counter();

            Counter rejectCounter = meterRegistry.find("rate_limit.reject.count")
                .tag("limit_type", limitType)
                .counter();

            if (passCounter != null && rejectCounter != null) {
                double pass = passCounter.count();
                double reject = rejectCounter.count();
                double total = pass + reject;

                if (total > 0) {
                    double passRate = (pass / total) * 100;
                    log.info("_[{}]_通过率:_{:.2f}%_(通过:_{},_拒绝:_{})",
                        limitType, passRate, (long)pass, (long)reject);
                }
            }
        }
    }
}
*/
