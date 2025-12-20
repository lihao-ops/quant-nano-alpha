package com.hao.strategyengine.resilience;

import jakarta.annotation.PostConstruct;
import lombok.Getter;
import lombok.ToString;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.util.Objects;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * 通用断路器服务，提供故障统计、状态流转与自愈能力。
 */
@Slf4j
@Service
public class CircuitBreakerService {

    private final ConcurrentMap<String, CircuitBreakerEntry> breakers = new ConcurrentHashMap<>();

    @Value("${circuit-breaker.failure-threshold:5}")
    private int defaultFailureThreshold;

    @Value("${circuit-breaker.open-state-duration-seconds:30}")
    private long defaultOpenStateSeconds;

    @Value("${circuit-breaker.half-open-success-threshold:3}")
    private int defaultHalfOpenSuccessThreshold;

    private CircuitBreakerConfig defaultConfig;

    @PostConstruct
    public void init() {
        defaultConfig = new CircuitBreakerConfig(
                defaultFailureThreshold,
                Duration.ofSeconds(Math.max(1, defaultOpenStateSeconds)),
                defaultHalfOpenSuccessThreshold
        );
        log.info("CircuitBreakerService_初始化完成:_defaultConfig={}_", defaultConfig);
    }

    /**
     * 注册或更新断路器配置。
     */
    public void registerBreaker(String name, CircuitBreakerConfig config) {
        Objects.requireNonNull(name, "breaker name cannot be null");
        breakers.compute(name, (k, entry) -> {
            if (entry == null) {
                return new CircuitBreakerEntry(config == null ? defaultConfig : config);
            }
            entry.updateConfig(config == null ? defaultConfig : config);
            return entry;
        });
    }

    /**
     * 判断是否允许请求通过。
     */
    public boolean allowRequest(String name) {
        CircuitBreakerEntry entry = breakers.computeIfAbsent(name, k -> new CircuitBreakerEntry(defaultConfig));
        return entry.allowRequest();
    }

    /**
     * 记录成功。
     */
    public void recordSuccess(String name) {
        CircuitBreakerEntry entry = breakers.computeIfAbsent(name, k -> new CircuitBreakerEntry(defaultConfig));
        entry.recordSuccess();
    }

    /**
     * 记录失败。
     */
    public void recordFailure(String name) {
        CircuitBreakerEntry entry = breakers.computeIfAbsent(name, k -> new CircuitBreakerEntry(defaultConfig));
        entry.recordFailure();
    }

    /**
     * 手动重置。
     */
    public void reset(String name) {
        CircuitBreakerEntry entry = breakers.computeIfAbsent(name, k -> new CircuitBreakerEntry(defaultConfig));
        entry.reset();
    }

    /**
     * 获取断路器状态快照。
     */
    public Optional<CircuitBreakerSnapshot> getSnapshot(String name) {
        CircuitBreakerEntry entry = breakers.get(name);
        if (entry == null) {
            return Optional.empty();
        }
        return Optional.of(entry.snapshot());
    }

    /**
     * 当前断路器数量。
     */
    public int size() {
        return breakers.size();
    }

    private enum State {
        CLOSED,
        OPEN,
        HALF_OPEN
    }

    private static final class CircuitBreakerEntry {
        private volatile CircuitBreakerConfig config;
        private volatile State state = State.CLOSED;
        private final AtomicInteger failureCount = new AtomicInteger();
        private final AtomicInteger halfOpenSuccessCount = new AtomicInteger();
        private volatile long stateSince = System.currentTimeMillis();

        private CircuitBreakerEntry(CircuitBreakerConfig config) {
            this.config = config;
        }

        private synchronized boolean allowRequest() {
            long now = System.currentTimeMillis();
            switch (state) {
                case CLOSED:
                    return true;
                case OPEN:
                    if (now - stateSince >= config.getOpenStateDuration().toMillis()) {
                        transitionToHalfOpen();
                        return true;
                    }
                    return false;
                case HALF_OPEN:
                    return halfOpenSuccessCount.get() < config.getHalfOpenSuccessThreshold();
                default:
                    return true;
            }
        }

        private synchronized void recordSuccess() {
            if (state == State.HALF_OPEN) {
                int success = halfOpenSuccessCount.incrementAndGet();
                if (success >= config.getHalfOpenSuccessThreshold()) {
                    transitionToClosed();
                }
            } else {
                failureCount.set(0);
            }
        }

        private synchronized void recordFailure() {
            if (state == State.HALF_OPEN) {
                transitionToOpen();
                return;
            }
            int currentFailures = failureCount.incrementAndGet();
            if (currentFailures >= config.getFailureThreshold()) {
                transitionToOpen();
            }
        }

        private synchronized void reset() {
            transitionToClosed();
        }

        private synchronized CircuitBreakerSnapshot snapshot() {
            return new CircuitBreakerSnapshot(state, failureCount.get(), halfOpenSuccessCount.get(), stateSince, config);
        }

        private void updateConfig(CircuitBreakerConfig newConfig) {
            this.config = newConfig;
        }

        private void transitionToClosed() {
            state = State.CLOSED;
            failureCount.set(0);
            halfOpenSuccessCount.set(0);
            stateSince = System.currentTimeMillis();
        }

        private void transitionToOpen() {
            state = State.OPEN;
            stateSince = System.currentTimeMillis();
            halfOpenSuccessCount.set(0);
        }

        private void transitionToHalfOpen() {
            state = State.HALF_OPEN;
            failureCount.set(0);
            halfOpenSuccessCount.set(0);
            stateSince = System.currentTimeMillis();
        }
    }

    /**
     * 断路器配置。
     */
    @Getter
    @ToString
    public static class CircuitBreakerConfig {
        private final int failureThreshold;
        private final Duration openStateDuration;
        private final int halfOpenSuccessThreshold;

        public CircuitBreakerConfig(int failureThreshold, Duration openStateDuration, int halfOpenSuccessThreshold) {
            if (failureThreshold <= 0) {
                throw new IllegalArgumentException("failureThreshold must be > 0");
            }
            if (halfOpenSuccessThreshold <= 0) {
                throw new IllegalArgumentException("halfOpenSuccessThreshold must be > 0");
            }
            this.failureThreshold = failureThreshold;
            this.openStateDuration = Objects.requireNonNull(openStateDuration, "openStateDuration cannot be null");
            this.halfOpenSuccessThreshold = halfOpenSuccessThreshold;
        }
    }

    /**
     * 断路器状态快照。
     */
    @Getter
    @ToString
    public static class CircuitBreakerSnapshot {
        private final String state;
        private final int failureCount;
        private final int halfOpenSuccessCount;
        private final long stateSince;
        private final CircuitBreakerConfig config;

        private CircuitBreakerSnapshot(State state, int failureCount, int halfOpenSuccessCount, long stateSince,
                                       CircuitBreakerConfig config) {
            this.state = state.name();
            this.failureCount = failureCount;
            this.halfOpenSuccessCount = halfOpenSuccessCount;
            this.stateSince = stateSince;
            this.config = config;
        }
    }
}
