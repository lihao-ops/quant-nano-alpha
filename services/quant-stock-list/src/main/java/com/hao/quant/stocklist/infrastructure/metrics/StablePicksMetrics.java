package com.hao.quant.stocklist.infrastructure.metrics;

import com.github.benmanes.caffeine.cache.Cache;
import com.github.benmanes.caffeine.cache.stats.CacheStats;
import com.hao.quant.stocklist.infrastructure.cache.CacheWrapper;
import io.micrometer.core.instrument.Gauge;
import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.binder.MeterBinder;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Component;

/**
 * 自定义监控指标,暴露缓存命中率等信息。
 * <p>
 * 通过 Micrometer 暴露多级缓存的关键指标,便于运维侧观察缓存健康状态。
 * </p>
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class StablePicksMetrics implements MeterBinder {

    private final Cache<String, CacheWrapper<?>> caffeineCache;
    private final RedisTemplate<String, Object> redisTemplate;

    /**
     * 将自定义指标注册到监控系统。
     */
    @Override
    public void bindTo(MeterRegistry registry) {
        Gauge.builder("stable_picks_cache_l1_size", caffeineCache, Cache::estimatedSize)
                .description("Caffeine 缓存 Key 数量")
                .register(registry);

        Gauge.builder("stable_picks_cache_l1_hit_rate", caffeineCache, cache -> {
                    CacheStats stats = cache.stats();
                    return stats.hitRate();
                })
                .description("Caffeine 缓存命中率")
                .register(registry);

        Gauge.builder("stable_picks_cache_l1_evictions", caffeineCache, cache -> cache.stats().evictionCount())
                .description("Caffeine 缓存淘汰次数")
                .register(registry);

        Gauge.builder("stable_picks_cache_l2_keys", this, metrics -> {
                    try {
                        // 使用 Redis keys 统计二级缓存数量,异常时降级为 0
                        var keys = redisTemplate.keys("stable:picks:*");
                        return keys != null ? keys.size() : 0;
                    } catch (Exception ex) {
                        log.error("获取RedisKey失败|Redis_key_fetch_failed", ex);
                        return 0;
                    }
                })
                .description("Redis 缓存 Key 数量")
                .register(registry);
    }
}
