package com.hao.quant.stocklist.domain.lock;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.redisson.api.RLock;
import org.redisson.api.RedissonClient;
import org.springframework.stereotype.Component;

import java.time.Duration;
import java.util.concurrent.TimeUnit;
import java.util.function.Supplier;

/**
 * 分布式锁封装,防止缓存击穿。
 * <p>
 * 通过 Redisson 提供的分布式锁能力保护缓存回源逻辑,避免同一 Key 并发打到数据库。
 * </p>
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class StablePicksLockManager {

    private final RedissonClient redissonClient;

    /**
     * 在分布式锁保护下执行回源逻辑。
     *
     * @param lockKey   锁标识
     * @param waitTime  获取锁的最长等待时间
     * @param leaseTime 锁租约时间
     * @param supplier  获取锁成功后的执行逻辑
     * @param fallback  获取锁失败或异常时的降级逻辑
     * @param <T>       返回结果类型
     * @return 执行结果
     */
    public <T> T executeWithLock(String lockKey, Duration waitTime, Duration leaseTime, Supplier<T> supplier, Supplier<T> fallback) {
        RLock lock = redissonClient.getLock(lockKey);
        try {
            boolean locked = lock.tryLock(waitTime.toMillis(), leaseTime.toMillis(), TimeUnit.MILLISECONDS);
            if (!locked) {
                log.warn("获取缓存锁失败: {}", lockKey);
                return fallback.get();
            }
            return supplier.get();
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            return fallback.get();
        } finally {
            if (lock.isHeldByCurrentThread()) {
                lock.unlock();
            }
        }
    }
}
