package com.hao.datacollector.integration.redis;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

/**
 * Redis Bloom Filter 测试类
 *
 * <p>测试目标：
 * 1. 清理布隆过滤器对应的 Redis 位图；
 * 2. 添加元素到布隆过滤器；
 * 3. 校验元素是否存在；
 * 4. 验证不存在的元素返回 false；
 * 5. 打印测试成功日志。</p>
 *
 * <p>注意：
 * - 布隆过滤器可能存在误判，但不会漏判；
 * - 测试使用单独的测试 key，避免污染正式数据。</p>
 */
@SpringBootTest
public class RedisBloomFilterTest {

    private static final Logger logger = LoggerFactory.getLogger(RedisBloomFilterTest.class);

    @Autowired
    private RedisBloomFilter redisBloomFilter; // 布隆过滤器工具类

    @Autowired
    private RedisConfig redisConfig; // Redis操作工具类

    // 测试使用的布隆过滤器key
    private static final String BLOOM_KEY = "test:bloom";

    @Test
    public void testAddAndMightContain() {
        // 1️⃣ 清理测试位图，确保布隆过滤器是空的
        redisConfig.del(BLOOM_KEY);
        logger.info("清理测试布隆过滤器 key: {}", BLOOM_KEY);
        // 2️⃣ 添加元素到布隆过滤器
        String element1 = "user:1001";
        String element2 = "user:1002";
        redisBloomFilter.add(BLOOM_KEY, element1);
        redisBloomFilter.add(BLOOM_KEY, element2);
        logger.info("添加元素到布隆过滤器: {}, {}", element1, element2);
        // 3️⃣ 校验已添加元素，应该返回 true
        boolean contains1 = redisBloomFilter.mightContain(BLOOM_KEY, element1);
        boolean contains2 = redisBloomFilter.mightContain(BLOOM_KEY, element2);
        logger.info("校验已添加元素是否存在: {} -> {}, {} -> {}", element1, contains1, element2, contains2);
        // 4️⃣ 校验未添加元素，应该返回 false（布隆过滤器可能误判为 true，但一般不会）
        String element3 = "user:9999";
        boolean contains3 = redisBloomFilter.mightContain(BLOOM_KEY, element3);
        logger.info("校验未添加元素是否存在: {} -> {}", element3, contains3);
        // 5️⃣ 断言结果
        Assertions.assertTrue(contains1, element1 + " 应该存在布隆过滤器中");
        Assertions.assertTrue(contains2, element2 + " 应该存在布隆过滤器中");
        Assertions.assertFalse(contains3, element3 + " 不应该存在布隆过滤器中");
        // ✅ 测试成功日志
        logger.info("✅ Redis布隆过滤器测试通过");
    }
}
