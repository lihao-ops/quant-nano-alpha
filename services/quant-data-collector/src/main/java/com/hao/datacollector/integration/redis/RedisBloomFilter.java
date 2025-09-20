package com.hao.datacollector.integration.redis;

import org.springframework.stereotype.Component;

/**
 * Redis 布隆过滤器工具类
 *
 * <p>用于防止缓存穿透，将数据 key 对应的可能存在信息存储在 Redis 位图中。
 * 使用多个哈希函数映射到不同位，通过 GETBIT/SETBIT 操作实现布隆过滤器。
 *
 * <p>特点：
 * 1. 支持任意 String 类型 key/value。
 * 2. 基于 Redis bitmap，实现轻量级、分布式布隆过滤器。
 * 3. 不会产生误删，仅可能有少量误判（返回存在但实际上不存在）。
 *
 * 使用示例：
 * <pre>
 *   // 判断 key 是否可能存在
 *   if (redisBloomFilter.mightContain("bloom:data", key)) {
 *       // 可能存在 → 访问缓存或数据库
 *   } else {
 *       // 一定不存在 → 拦截请求
 *   }
 *
 *   // 添加新的 key 到布隆过滤器
 *   redisBloomFilter.add("bloom:data", key);
 * </pre>
 *
 * 注意：
 * - BLOOM_SIZE 应根据业务量预估，过小会增加误判率。
 * - HASH_SEEDS 可调整哈希函数数量，数量越多误判率越低，但写入成本略增。
 *
 * @author hli
 * @version 1.0
 * @since 2025-09-20
 */
@Component
public class RedisBloomFilter {

    private final RedisConfig redisConfig;

    /** 位图大小，默认 2^24 位，可根据实际业务调整 */
    private static final int BLOOM_SIZE = 1 << 24;

    /** 多个哈希种子，实现多哈希函数 */
    private static final int[] HASH_SEEDS = {7, 11, 13, 31, 37, 61};

    public RedisBloomFilter(RedisConfig redisConfig) {
        this.redisConfig = redisConfig;
    }

    /**
     * 哈希函数
     *
     * @param value 需要映射的字符串
     * @param seed 哈希种子
     * @return 对应位图索引
     */
    private int hash(String value, int seed) {
        int result = 0;
        for (char c : value.toCharArray()) {
            result = result * seed + c;
        }
        // 限定在位图范围内
        return (BLOOM_SIZE - 1) & result;
    }

    /**
     * 判断布隆过滤器中是否可能包含指定 value
     *
     * @param key Redis 中的位图 key
     * @param value 待检测的字符串
     * @return true → 可能存在（需继续访问缓存/DB）
     *         false → 一定不存在（可直接拦截）
     */
    public boolean mightContain(String key, String value) {
        for (int seed : HASH_SEEDS) {
            int pos = hash(value, seed);
            // GETBIT 查询位是否为 1
            if (!Boolean.TRUE.equals(redisConfig.getRedisTemplate().opsForValue().getBit(key, pos))) {
                return false; // 任意一个 bit 为 0 → 一定不存在
            }
        }
        return true; // 所有 bit 都为 1 → 可能存在
    }

    /**
     * 将 value 添加到布隆过滤器
     *
     * @param key Redis 中的位图 key
     * @param value 待添加的字符串
     */
    public void add(String key, String value) {
        for (int seed : HASH_SEEDS) {
            int pos = hash(value, seed);
            // SETBIT 将对应 bit 置为 1
            redisConfig.getRedisTemplate().opsForValue().setBit(key, pos, true);
        }
    }
}
