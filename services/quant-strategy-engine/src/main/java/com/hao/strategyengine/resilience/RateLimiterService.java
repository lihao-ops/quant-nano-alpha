package com.hao.strategyengine.resilience;

import com.google.common.util.concurrent.RateLimiter;
import jakarta.annotation.PostConstruct;
import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.script.DefaultRedisScript;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.util.Collections;
import java.util.Objects;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.TimeUnit;

/**
 * 通用限流服务，封装分布式滑动窗口与本地令牌桶两种实现，
 * 提供全局 / 用户 / 策略类型等多维度限流能力。
 */
@Slf4j
@Service
public class RateLimiterService {

    private static final String LUA_SCRIPT =
            "local key = KEYS[1]\n" +
            "local window = tonumber(ARGV[1])\n" +
            "local limit = tonumber(ARGV[2])\n" +
            "local now = tonumber(ARGV[3])\n" +
            "local permits = tonumber(ARGV[4])\n" +
            "local windowStart = now - window * 1000\n" +
            "redis.call('ZREMRANGEBYSCORE', key, 0, windowStart)\n" +
            "local current = redis.call('ZCARD', key)\n" +
            "if current + permits <= limit then\n" +
            "    for i = 1, permits do\n" +
            "        local member = now .. '-' .. i\n" +
            "        redis.call('ZADD', key, now, member)\n" +
            "    end\n" +
            "    redis.call('EXPIRE', key, window + 1)\n" +
            "    return 1\n" +
            "else\n" +
            "    return 0\n" +
            "end";

    private static final double MIN_RATE = 0.01d;

    @Resource
    private StringRedisTemplate stringRedisTemplate;

    @Value("${rate-limit.distributed.enabled:true}")
    private boolean distributedEnabled;

    @Value("${rate-limit.redis.key-prefix:quant:rate_limit:}")
    private String redisKeyPrefix;

    @Value("${rate-limit.default-window-seconds:1}")
    private long defaultWindowSeconds;

    private DefaultRedisScript<Long> rateLimitScript;

    private Duration defaultWindow;

    private final ConcurrentMap<String, RateLimiter> localLimiters = new ConcurrentHashMap<>();

    @PostConstruct
    public void init() {
        defaultWindow = Duration.ofSeconds(Math.max(1, defaultWindowSeconds));
        rateLimitScript = new DefaultRedisScript<>();
        rateLimitScript.setScriptText(LUA_SCRIPT);
        rateLimitScript.setResultType(Long.class);
        log.info("RateLimiterService 初始化完成: distributedEnabled={}, defaultWindow={}s", distributedEnabled,
                defaultWindow.getSeconds());
    }

    /**
     * 获取全局限流许可。
     */
    public boolean tryAcquireGlobal(int limitPerWindow) {
        return tryAcquire("global", 1, limitPerWindow, defaultWindow);
    }

    /**
     * 获取全局限流许可，支持自定义窗口。
     */
    public boolean tryAcquireGlobal(int permits, int limitPerWindow, Duration window) {
        return tryAcquire("global", permits, limitPerWindow, window);
    }

    /**
     * 获取指定用户的限流许可。
     */
    public boolean tryAcquireForUser(Object userId, int limitPerWindow) {
        Objects.requireNonNull(userId, "userId cannot be null");
        return tryAcquire("user:" + userId, 1, limitPerWindow, defaultWindow);
    }

    /**
     * 获取指定用户的限流许可，支持自定义窗口与许可数。
     */
    public boolean tryAcquireForUser(Object userId, int permits, int limitPerWindow, Duration window) {
        Objects.requireNonNull(userId, "userId cannot be null");
        return tryAcquire("user:" + userId, permits, limitPerWindow, window);
    }

    /**
     * 按策略类型限流。
     */
    public boolean tryAcquireForStrategy(String strategyType, int limitPerWindow) {
        Objects.requireNonNull(strategyType, "strategyType cannot be null");
        return tryAcquire("strategy:" + strategyType, 1, limitPerWindow, defaultWindow);
    }

    /**
     * 通用限流入口，调用方只需提供限流维度后缀即可。
     */
    public boolean tryAcquire(String keySuffix, int permits, int limitPerWindow, Duration window) {
        if (limitPerWindow <= 0 || permits <= 0) {
            return false;
        }
        Duration targetWindow = normalizeWindow(window);
        String redisKey = buildKey(keySuffix);
        if (distributedEnabled && stringRedisTemplate != null) {
            Boolean distributedPass = tryAcquireDistributed(redisKey, permits, limitPerWindow, targetWindow);
            if (distributedPass != null) {
                return distributedPass;
            }
            log.warn("分布式限流执行异常，降级到本地限流: key={} permits={} limit={} window={}", redisKey, permits, limitPerWindow,
                    targetWindow);
        }
        return tryAcquireLocal(redisKey, permits, limitPerWindow, targetWindow);
    }

    /**
     * 预热/手动调整某个本地限流器的速率。
     */
    public void updateLocalLimiter(String keySuffix, double permitsPerSecond) {
        String redisKey = buildKey(keySuffix);
        RateLimiter limiter = localLimiters.compute(redisKey, (k, existing) -> {
            double safeRate = Math.max(permitsPerSecond, MIN_RATE);
            if (existing == null) {
                return RateLimiter.create(safeRate);
            }
            if (Math.abs(existing.getRate() - safeRate) > 0.01d) {
                existing.setRate(safeRate);
            }
            return existing;
        });
        if (limiter != null) {
            log.info("更新本地限流器: key={} rate={}", redisKey, limiter.getRate());
        }
    }

    /**
     * 删除本地限流器缓存。
     */
    public void removeLocalLimiter(String keySuffix) {
        String redisKey = buildKey(keySuffix);
        localLimiters.remove(redisKey);
        log.info("移除本地限流器: key={}", redisKey);
    }

    /**
     * 获取本地限流器信息。
     */
    public Optional<Double> getLocalLimiterRate(String keySuffix) {
        RateLimiter limiter = localLimiters.get(buildKey(keySuffix));
        return Optional.ofNullable(limiter).map(RateLimiter::getRate);
    }

    private Boolean tryAcquireDistributed(String redisKey, int permits, int limitPerWindow, Duration window) {
        try {
            Long result = stringRedisTemplate.execute(
                    rateLimitScript,
                    Collections.singletonList(redisKey),
                    String.valueOf(window.getSeconds()),
                    String.valueOf(limitPerWindow),
                    String.valueOf(System.currentTimeMillis()),
                    String.valueOf(permits)
            );
            return Long.valueOf(1).equals(result);
        } catch (Exception ex) {
            log.error("分布式限流执行失败: key={} permits={} limit={} window={}", redisKey, permits, limitPerWindow, window, ex);
            return null;
        }
    }

    private boolean tryAcquireLocal(String redisKey, int permits, int limitPerWindow, Duration window) {
        double permitsPerSecond = computePermitsPerSecond(limitPerWindow, window);
        RateLimiter limiter = localLimiters.compute(redisKey, (k, existing) -> {
            if (existing == null) {
                return RateLimiter.create(permitsPerSecond);
            }
            if (Math.abs(existing.getRate() - permitsPerSecond) > 0.01d) {
                existing.setRate(permitsPerSecond);
            }
            return existing;
        });
        boolean acquired = limiter.tryAcquire(permits, 100, TimeUnit.MILLISECONDS);
        if (!acquired) {
            log.debug("本地限流拒绝: key={} permits={} rate={}pps", redisKey, permits, limiter.getRate());
        }
        return acquired;
    }

    private Duration normalizeWindow(Duration window) {
        if (window == null || window.isZero() || window.isNegative()) {
            return defaultWindow;
        }
        return window;
    }

    private String buildKey(String keySuffix) {
        return redisKeyPrefix + keySuffix;
    }

    private double computePermitsPerSecond(int limitPerWindow, Duration window) {
        double windowSeconds = Math.max(1d, window.toMillis() / 1000d);
        return Math.max(limitPerWindow / windowSeconds, MIN_RATE);
    }
}
