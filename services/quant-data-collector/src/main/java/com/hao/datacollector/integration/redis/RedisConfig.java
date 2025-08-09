package com.hao.datacollector.integration.redis;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.redis.connection.RedisStandaloneConfiguration;
import org.springframework.data.redis.connection.lettuce.LettuceConnectionFactory;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.ZSetOperations;

import java.time.Duration;
import java.util.*;
import java.util.stream.Collectors;

/**
 * Redis 客户端通用配置类（适用于外部标准 Redis 服务）
 * <p>
 * 用于初始化 Redis 客户端并提供 Redis 常用操作能力。
 * 实现 RedisClient 接口，可注入使用。
 * </p>
 * 配置项来自 application.yml:
 * redis.host, redis.port, redis.password
 *
 * @author hli
 * @date 2025-08-07
 */
@Slf4j
@Configuration("RedisClient")
public class RedisConfig implements RedisClient<String>, InitializingBean {

    /**
     * 健康检查需要标准配置：Spring Boot的RedisReactiveHealthIndicator使用标准的spring.data.redis.*配置路径来创建连接进行健康检查
     * 故此为保持一直使用一套Redis配置官方标准的${spring.data.redis.
     */
    @Value("${spring.data.redis.host}")
    private String redisHost;

    @Value("${spring.data.redis.port}")
    private int redisPort;

    @Value("${spring.data.redis.password:}")
    private String redisPassword;

    private StringRedisTemplate redisTemplate;

    /**
     * 初始化 Redis 连接工厂和 StringRedisTemplate
     */
    @Override
    public void afterPropertiesSet() {
        try {
            log.info("Redis_config:host={},port={},password={}", redisHost, redisPort, redisPassword == null || redisPassword.isEmpty() ? "空" : "已配置");
            RedisStandaloneConfiguration config = new RedisStandaloneConfiguration(redisHost, redisPort);
            if (redisPassword != null && !redisPassword.isEmpty()) {
                config.setPassword(redisPassword);
            }
            LettuceConnectionFactory factory = new LettuceConnectionFactory(config);
            factory.afterPropertiesSet();

            redisTemplate = new StringRedisTemplate();
            redisTemplate.setConnectionFactory(factory);
            redisTemplate.afterPropertiesSet();

            log.info("Redis 初始化成功，连接到 {}:{}", redisHost, redisPort);
            // 测试连接
            String testResult = redisTemplate.opsForValue().get("test");
            redisTemplate.opsForValue().set("connection-test", "success", Duration.ofSeconds(10));
            log.info("Redis连接测试成功");

        } catch (Exception e) {
            log.error("Redis连接测试失败: {}", e.getMessage(), e);
        }
    }


    /**
     * 设置 Key 的值并指定过期时间（秒）
     */
    @Override
    public void set(String key, String value, int expireTime) {
        redisTemplate.opsForValue().set(key, value);
        redisTemplate.expire(key, java.time.Duration.ofSeconds(expireTime));
    }

    /**
     * 获取指定 Key 的值
     */
    @Override
    public String get(String key) {
        return redisTemplate.opsForValue().get(key);
    }

    /**
     * 设置 Key 的值（不过期）
     */
    @Override
    public void set(String key, String value) {
        redisTemplate.opsForValue().set(key, value);
    }

    /**
     * 对 Key 的值执行自增操作
     */
    @Override
    public Long incr(String key) {
        return redisTemplate.opsForValue().increment(key);
    }

    /**
     * 对 Key 的值执行自减操作
     */
    @Override
    public Long decr(String key) {
        return redisTemplate.opsForValue().decrement(key);
    }

    /**
     * 删除指定 Key
     */
    @Override
    public Long del(String key) {
        return redisTemplate.delete(key) ? 1L : 0L;
    }

    /**
     * 删除多个 Key
     */
    @Override
    public Long del(String[] keys) {
        return redisTemplate.delete(List.of(keys));
    }

    /**
     * 设置 Hash 键值对
     */
    @Override
    public void hset(String key, String field, String value) {
        redisTemplate.opsForHash().put(key, field, value);
    }

    /**
     * 仅当 Hash 的 field 不存在时设置值
     */
    @Override
    public void hsetnx(String key, String field, String value) {
        redisTemplate.opsForHash().putIfAbsent(key, field, value);
    }

    /**
     * 获取 Hash 中指定 field 的值
     */
    @Override
    public String hget(String key, String field) {
        Object val = redisTemplate.opsForHash().get(key, field);
        return val != null ? val.toString() : null;
    }

    /**
     * 获取整个 Hash 的所有字段和值
     */
    @Override
    public Map<String, String> hgetAll(String key) {
        return (Map) redisTemplate.opsForHash().entries(key);
    }

    /**
     * 设置 Hash 多个字段
     */
    @Override
    public void hmset(String key, Map<String, String> paramMap) {
        redisTemplate.opsForHash().putAll(key, paramMap);
    }

    /**
     * 获取 Hash 多个字段的值
     */
    @Override
    public List<String> hmget(String key, String... fields) {
        return (List) redisTemplate.opsForHash().multiGet(key, List.of(fields));
    }

    /**
     * 获取 Hash 所有字段名
     */
    @Override
    public Set<String> hkeys(String key) {
        Set<Object> keys = redisTemplate.opsForHash().keys(key);
        if (keys == null) {
            return Collections.emptySet();
        }
        return keys.stream().map(Object::toString).collect(Collectors.toSet());
    }


    /**
     * 删除 Hash 的一个或多个字段
     */
    @Override
    public long hdel(String key, String... fields) {
        return redisTemplate.opsForHash().delete(key, (Object[]) fields);
    }

    /**
     * 设置 Key 的过期时间（秒）
     */
    @Override
    public long expire(String key, int seconds) {
        return redisTemplate.expire(key, java.time.Duration.ofSeconds(seconds)) ? 1L : 0L;
    }

    /**
     * 获取 Key 的剩余存活时间（秒）
     */
    @Override
    public long ttl(String key) {
        return redisTemplate.getExpire(key);
    }

    /**
     * 向列表左侧插入元素
     */
    @Override
    public Long lpush(String key, String... values) {
        return redisTemplate.opsForList().leftPushAll(key, values);
    }

    /**
     * 从列表右侧弹出元素
     */
    @Override
    public String rpop(String key) {
        return redisTemplate.opsForList().rightPop(key);
    }

    /**
     * 以下 zset 方法未实现，如需请补充
     */
    /**
     * 向有序集合添加多个元素
     */
    @Override
    public long zadd(String key, Map<String, Double> valueMap) {
        if (valueMap == null || valueMap.isEmpty()) return 0;
        Set<ZSetOperations.TypedTuple<String>> tuples = new HashSet<>();
        for (Map.Entry<String, Double> entry : valueMap.entrySet()) {
            tuples.add(ZSetOperations.TypedTuple.of(entry.getKey(), entry.getValue()));
        }
        return redisTemplate.opsForZSet().add(key, tuples);
    }

    /**
     * 向有序集合添加一个元素
     */
    @Override
    public long zadd(String key, double score, String member) {
        Boolean added = redisTemplate.opsForZSet().add(key, member, score);
        return Boolean.TRUE.equals(added) ? 1L : 0L;
    }

    /**
     * 获取指定区间内的元素（升序）
     */
    @Override
    public Set<String> zrange(String key, long start, long stop) {
        return redisTemplate.opsForZSet().range(key, start, stop);
    }

    /**
     * 获取指定区间内的元素（降序）
     */
    @Override
    public Set<String> zrevrange(String key, long start, long stop) {
        return redisTemplate.opsForZSet().reverseRange(key, start, stop);
    }

    /**
     * 删除指定分数区间内的元素
     */
    @Override
    public long zremrangeByScore(String key, double scoreMin, double scoreMax) {
        return redisTemplate.opsForZSet().removeRangeByScore(key, scoreMin, scoreMax);
    }

    /**
     * 获取指定成员的分数
     */
    @Override
    public Double zscore(String key, String member) {
        return redisTemplate.opsForZSet().score(key, member);
    }

    /**
     * 增加成员的分数
     */
    @Override
    public Double zincrby(String key, double increment, String member) {
        return redisTemplate.opsForZSet().incrementScore(key, member, increment);
    }

}
