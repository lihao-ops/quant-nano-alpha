package com.hao.quant.stocklist.infrastructure.cache;

import com.github.benmanes.caffeine.cache.Cache;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Component;

import java.time.Duration;
import java.util.Optional;

/**
 * 封装多级缓存读写逻辑。
 * <p>
 * 对外提供 L1 (Caffeine) + L2 (Redis) 的访问方法,便于领域服务复用。
 * </p>
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class StablePicksCacheRepository {

    private final Cache<String, CacheWrapper<?>> caffeineCache;
    private final RedisTemplate<String, Object> redisTemplate;

    @SuppressWarnings("unchecked")
    /**
     * 从本地 Caffeine 缓存读取。
     */
    public <T> Optional<CacheWrapper<T>> getLocal(String cacheKey) {
        return Optional.ofNullable((CacheWrapper<T>) caffeineCache.getIfPresent(cacheKey));
    }

    @SuppressWarnings("unchecked")
    /**
     * 从 Redis 缓存读取。
     */
    public <T> Optional<CacheWrapper<T>> getDistributed(String cacheKey) {
        Object value = redisTemplate.opsForValue().get(cacheKey);
        if (value == null) {
            return Optional.empty();
        }
        try {
            return Optional.of((CacheWrapper<T>) value);
        } catch (ClassCastException ex) {
            log.warn("缓存类型不匹配|Cache_type_mismatch,cacheKey={}", cacheKey);
            redisTemplate.delete(cacheKey);
            return Optional.empty();
        }
    }

    /**
     * 写入本地与 Redis 缓存。
     */
    public void put(String cacheKey, CacheWrapper<?> wrapper, Duration redisTtl) {
        caffeineCache.put(cacheKey, wrapper);
        redisTemplate.opsForValue().set(cacheKey, wrapper, redisTtl);
    }

    /**
     * 仅更新本地缓存。
     */
    public void putLocal(String cacheKey, CacheWrapper<?> wrapper) {
        caffeineCache.put(cacheKey, wrapper);
    }

    /**
     * 同步移除本地与 Redis 缓存。
     */
    public void evict(String cacheKey) {
        caffeineCache.invalidate(cacheKey);
        redisTemplate.delete(cacheKey);
    }
}
