package com.hao.strategyengine.common.cache;

import com.alibaba.fastjson.JSON;
import com.hao.strategyengine.model.response.StrategyResult;
import com.hao.strategyengine.model.response.StrategyResultBundle;
import lombok.RequiredArgsConstructor;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.util.concurrent.ThreadLocalRandom;
import java.util.function.Supplier;

/**
 * ===============================================================
 * 【类名】：StrategyCacheService（策略结果缓存服务）
 * ===============================================================
 *
 * 【功能定位】：
 *   ⦿ 本类封装了策略结果在 Redis 中的读写逻辑。
 *   ⦿ 提供两类缓存能力：
 *       ① 单策略结果缓存（getOrCompute）
 *       ② 组合策略结果缓存（save）
 *
 * 【核心思路】：
 *   - 优先读取 Redis 缓存；
 *   - 若不存在则执行 supplier 计算；
 *   - 将结果 JSON 化后写入 Redis；
 *   - 写入时增加随机 TTL 防止缓存雪崩。
 *
 * 【执行流程位置】：
 *   属于系统执行链的「第 5 层」（策略执行后的缓存层）：
 *   Controller → Service → Facade → Dispatcher → Strategy → ✅ Cache
 */
@Service
@RequiredArgsConstructor
public class StrategyCacheService {

    /** Redis 客户端（Spring 提供的字符串模板） */
    private final StringRedisTemplate redis;

    /** 基础 TTL，随机增加少量偏移防止缓存雪崩 */
    private static final Duration BASE_TTL = Duration.ofMinutes(5);

    /**
     * ===============================================================
     * 【方法名】：getOrCompute
     * ===============================================================
     *
     * 【功能】：
     *   获取单个策略结果缓存；若不存在，则计算并写入缓存。
     *
     * 【参数】：
     *   @param key       缓存键（如 "strategy:MA:600519"）
     *   @param supplier  当缓存缺失时执行的计算逻辑
     *
     * 【返回】：
     *   StrategyResult（单策略结果）
     *
     * 【使用位置】：
     *   Dispatcher 内部或单策略执行时调用
     */
    public StrategyResult getOrCompute(String key, Supplier<StrategyResult> supplier) {
        // Step 1️⃣ 从缓存获取
        String s = redis.opsForValue().get(key);
        if (s != null) {
            try {
                return JSON.parseObject(s, StrategyResult.class);
            } catch (Exception ignored) {}
        }

        // Step 2️⃣ 执行计算逻辑
        StrategyResult v = supplier.get();

        // Step 3️⃣ 计算随机 TTL 并写入 Redis
        Duration ttl = BASE_TTL.plusSeconds(ThreadLocalRandom.current().nextInt(0, 60));
        redis.opsForValue().set(key, JSON.toJSONString(v), ttl);

        return v;
    }

    /**
     * ===============================================================
     * 【方法名】：save
     * ===============================================================
     *
     * 【功能】：
     *   缓存整个组合策略结果（StrategyResultBundle）。
     *
     * 【参数】：
     *   @param bundle 聚合后的策略执行结果包
     *
     * 【设计说明】：
     *   - 主要用于 Facade 在分布式锁计算完成后写入缓存；
     *   - key 通常为组合 key（如 "lock:combo:MA_MOM_DRAGON_TWO"）；
     *   - 仅负责缓存，不参与回源计算。
     *
     * 【使用位置】：
     *   Facade -> compute supplier 内（策略全部计算完成后）
     */
    public void save(StrategyResultBundle bundle) {
        if (bundle == null || bundle.getComboKey() == null) {
            return;
        }
        String key = "bundle:" + bundle.getComboKey();
        Duration ttl = BASE_TTL.plusSeconds(ThreadLocalRandom.current().nextInt(0, 60));
        redis.opsForValue().set(key, JSON.toJSONString(bundle), ttl);
    }
}
