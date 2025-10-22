package com.hao.strategyengine.strategy.lock;

import com.hao.strategyengine.model.response.StrategyResultBundle;
import lombok.RequiredArgsConstructor;
import org.redisson.api.RLock;
import org.redisson.api.RedissonClient;
import org.springframework.stereotype.Service;

import java.util.Map;
import java.util.concurrent.*;
import java.util.function.Supplier;

/**
 * ===============================================================
 * 【类名】：DistributedLockService（分布式锁协调服务）
 * ===============================================================
 *
 * 【功能定位】：
 *   ⦿ 本类用于控制“策略组合计算”的并发执行。
 *   ⦿ 核心目标：确保同一组合（comboKey）在多实例环境下只被计算一次。
 *
 * 【核心思路】：
 *   - 使用 Redisson 实现 Redis 分布式锁；
 *   - 若当前线程成功获取锁 → 执行计算（compute.get()）；
 *   - 若已有线程在计算 → 等待其完成，通过 CompletableFuture 获取结果；
 *   - 所有等待者共享一个 pending future，避免重复计算。
 *
 * 【调用位置】：
 *   ◉ Facade 层（StrategyEngineFacade） → Step 4 调用
 *   Controller → Service → Facade → ✅ LockService → Dispatcher
 *
 * 【执行流程】：
 *   ┌────────────────────────────────────────┐
 *   │ Step 1：尝试获取分布式锁（Redisson.tryLock）       │
 *   │ Step 2：若成功 → 执行 compute 逻辑并广播结果         │
 *   │ Step 3：若失败 → 等待已有 pending 任务完成（Future） │
 *   │ Step 4：结果返回或超时抛出异常                        │
 *   └────────────────────────────────────────┘
 *
 * 【优势】：
 *   ✅ 防止同一策略组合被多节点重复计算（幂等保证）
 *   ✅ 支持集群下锁自动过期释放（30秒 lease）
 *   ✅ 轻量实现的 Future 等待机制，节省系统资源
 *
 * 【局限性】：
 *   ⚠️ 若计算耗时超过 30 秒，需要手动续期或调整 leaseTime；
 *   ⚠️ 当前实现为非可重入锁（同线程重复获取需谨慎）。
 */
@Service
@RequiredArgsConstructor
public class DistributedLockService {

    /** Redisson 客户端实例：提供分布式锁 API */
    private final RedissonClient redisson;

    /**
     * 本地等待映射表：
     *   - key：组合策略 key（comboKey）
     *   - value：等待结果的 Future（多个线程共享等待）
     */
    private final Map<String, CompletableFuture<StrategyResultBundle>> pending = new ConcurrentHashMap<>();

    /**
     * ===============================================================
     * 【方法名】：acquireOrWait
     * ===============================================================
     *
     * 【功能】：
     *   控制策略组合的并发执行，确保每个 comboKey 在全局只被计算一次。
     *
     * 【参数】：
     *   @param comboKey  策略组合唯一标识（如 "MA_MOM_DRAGON_TWO"）
     *   @param compute   计算逻辑（通常为 Facade 中定义的 Supplier）
     *
     * 【返回】：
     *   StrategyResultBundle（策略结果聚合包）
     *
     * 【执行逻辑】：
     *   ① 尝试立即获取锁（非阻塞，leaseTime=30s）
     *   ② 若成功：执行 compute 并唤醒等待者
     *   ③ 若失败：阻塞等待 pending future 结果
     *   ④ 若等待超时：抛出 RuntimeException
     */
    public StrategyResultBundle acquireOrWait(String comboKey, Supplier<StrategyResultBundle> compute) {
        // Step 1️⃣ 拼接分布式锁 key
        String lockName = "lock:combo:" + comboKey;
        RLock lock = redisson.getLock(lockName);

        boolean acquired = false;
        try {
            // Step 2️⃣ 尝试立即获取锁（非阻塞），自动过期 30 秒
            acquired = lock.tryLock(0, 30, TimeUnit.SECONDS);

            if (acquired) {
                // Step 3️⃣ 当前线程成功获取锁 → 执行策略计算
                StrategyResultBundle result = compute.get();

                // Step 4️⃣ 唤醒等待中的任务（如果存在）
                CompletableFuture<StrategyResultBundle> waiting = pending.remove(comboKey);
                if (waiting != null) {
                    waiting.complete(result);
                }

                return result;
            } else {
                // Step 5️⃣ 已有线程在计算 → 等待 pending future
                CompletableFuture<StrategyResultBundle> future =
                        pending.computeIfAbsent(comboKey, k -> new CompletableFuture<>());

                try {
                    // Step 6️⃣ 等待计算结果（最多等待 5 秒）
                    return future.get(5, TimeUnit.SECONDS);
                } catch (TimeoutException te) {
                    throw new RuntimeException("等待计算超时：" + comboKey);
                } catch (ExecutionException e) {
                    throw new RuntimeException("计算执行异常：" + comboKey, e);
                }
            }
        } catch (InterruptedException ie) {
            Thread.currentThread().interrupt();
            throw new RuntimeException("线程中断：" + comboKey, ie);
        } finally {
            // Step 7️⃣ 若当前线程持有锁，释放锁
            if (acquired) {
                try {
                    lock.unlock();
                } catch (Exception ignored) {
                }
            }
        }
    }
}
