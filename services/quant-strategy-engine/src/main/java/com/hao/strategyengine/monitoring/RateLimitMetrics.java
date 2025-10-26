package com.hao.strategyengine.monitoring;

import io.micrometer.core.instrument.*;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.EnableScheduling;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import jakarta.annotation.Resource;
import java.nio.charset.StandardCharsets;
import java.util.concurrent.TimeUnit;

@Slf4j
@Component
@EnableScheduling
public class RateLimitMetrics {

    @Resource
    private MeterRegistry meterRegistry;

    /**
     * 记录限流拒绝事件
     */
    public void recordRateLimitReject(String limitType, String userId, String strategyType) {
        Counter.builder("rate_limit.reject.count")
                .description("限流拒绝次数统计")
                .tag("limit_type", limitType)
                .tag("strategy_type", strategyType)
                .tag("user_id", hashUserId(userId))
                .register(meterRegistry)
                .increment();
    }

    /**
     * 记录等待时间
     */
    public void recordWaitTime(String limitType, long waitMillis) {
        Timer.builder("rate_limit.wait_time")
                .description("限流等待耗时分布")
                .tag("limit_type", limitType)
                .publishPercentiles(0.5, 0.9, 0.99) // 提供 P50, P90, P99
                .register(meterRegistry)
                .record(waitMillis, TimeUnit.MILLISECONDS);
    }

    /**
     * 每10秒输出一次当前P99延迟
     */
    @Scheduled(fixedRate = 10000)
    public void reportP99() {
        Timer timer = meterRegistry.find("rate_limit.wait_time").timer();
        if (timer != null) {
            double p99 = timer.takeSnapshot().percentileValues()[2].value(TimeUnit.MILLISECONDS);
            log.info("📊 限流等待时间P99: {} ms", String.format("%.2f", p99));
        }
    }

    /**
     * 用户ID脱敏（hash）
     */
    private String hashUserId(String userId) {
        if (userId == null) return "unknown";
        int hash = Math.abs(userId.hashCode());
        return Integer.toHexString(hash % 10000); // 简单脱敏
    }
}