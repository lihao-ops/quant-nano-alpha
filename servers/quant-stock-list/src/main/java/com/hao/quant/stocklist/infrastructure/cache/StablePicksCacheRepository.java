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
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class StablePicksCacheRepository {

    private final Cache<String, CacheWrapper<?>> caffeineCache;
    private final RedisTemplate<String, Object> redisTemplate;

    @SuppressWarnings("unchecked")
    public <T> Optional<CacheWrapper<T>> getLocal(String cacheKey) {
        return Optional.ofNullable((CacheWrapper<T>) caffeineCache.getIfPresent(cacheKey));
    }

    @SuppressWarnings("unchecked")
    public <T> Optional<CacheWrapper<T>> getDistributed(String cacheKey) {
        Object value = redisTemplate.opsForValue().get(cacheKey);
        if (value == null) {
            return Optional.empty();
        }
        try {
            return Optional.of((CacheWrapper<T>) value);
        } catch (ClassCastException ex) {
            log.warn("缓存类型不匹配,删除Redis Key: {}", cacheKey);
            redisTemplate.delete(cacheKey);
            return Optional.empty();
        }
    }

    public void put(String cacheKey, CacheWrapper<?> wrapper, Duration redisTtl) {
        caffeineCache.put(cacheKey, wrapper);
        redisTemplate.opsForValue().set(cacheKey, wrapper, redisTtl);
    }

    public void putLocal(String cacheKey, CacheWrapper<?> wrapper) {
        caffeineCache.put(cacheKey, wrapper);
    }

    public void evict(String cacheKey) {
        caffeineCache.invalidate(cacheKey);
        redisTemplate.delete(cacheKey);
    }
}
