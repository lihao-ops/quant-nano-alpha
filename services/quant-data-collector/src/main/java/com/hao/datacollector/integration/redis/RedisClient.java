package com.hao.datacollector.integration.redis;

import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * 通用 Redis 客户端操作接口
 * 支持 String、Hash、List、ZSet 等基础 Redis 数据结构的常用命令封装
 *
 * @param <T> 泛型，一般为 String
 */
public interface RedisClient<T> {

    /** ------------------ String 操作 ------------------ **/

    /**
     * 保存数据到 Redis，并设置失效时间（秒）
     *
     * @param key        key
     * @param value      value
     * @param expireTime 失效时间（单位：秒）
     */
    void set(String key, T value, int expireTime);

    /**
     * 保存数据到 Redis（无过期时间）
     *
     * @param key   key
     * @param value value
     */
    void set(String key, T value);

    /**
     * 获取指定 key 的值
     *
     * @param key key
     * @return 值
     */
    T get(String key);

    /**
     * 自增 1
     *
     * @param key key
     * @return 增加后的值
     */
    Long incr(String key);

    /**
     * 自减 1
     *
     * @param key key
     * @return 减少后的值
     */
    Long decr(String key);

    /**
     * 删除指定 key
     *
     * @param key key
     * @return 成功删除的个数
     */
    Long del(String key);

    /**
     * 批量删除多个 key
     *
     * @param keys key 数组
     * @return 成功删除的个数
     */
    Long del(String[] keys);


    /** ------------------ Hash 操作 ------------------ **/

    /**
     * 设置 Hash 值（key -> field -> value）
     *
     * @param key   hash key
     * @param field hash field
     * @param value hash value
     */
    void hset(String key, String field, String value);

    /**
     * 当且仅当 field 不存在时设置 hash 值
     *
     * @param key   hash key
     * @param field hash field
     * @param value hash value
     */
    void hsetnx(String key, String field, String value);

    /**
     * 获取 hash 中 field 对应的值
     *
     * @param key   hash key
     * @param field hash field
     * @return 对应值
     */
    String hget(String key, T field);

    /**
     * 获取指定 hash 的全部字段和值
     *
     * @param key hash key
     * @return map
     */
    Map<String, String> hgetAll(String key);

    /**
     * 批量设置 hash 多个字段
     *
     * @param key      hash key
     * @param paramMap field -> value map
     */
    void hmset(String key, Map<String, String> paramMap);

    /**
     * 批量获取多个字段的值
     *
     * @param key       hash key
     * @param paramArgs field 数组
     * @return 值集合
     */
    List<String> hmget(String key, String... paramArgs);

    /**
     * 获取指定 hash 的所有字段名
     *
     * @param key hash key
     * @return 所有字段名
     */
    Set<String> hkeys(String key);

    /**
     * 删除 hash 中一个或多个字段
     *
     * @param key   hash key
     * @param field 字段数组
     * @return 删除数量
     */
    long hdel(String key, String... field);


    /** ------------------ ZSet 操作 ------------------ **/

    /**
     * 添加多个成员到 ZSet，有序集合
     *
     * @param key      zset key
     * @param valueMap member -> score 映射
     * @return 添加成功的个数
     */
    long zadd(String key, Map<String, Double> valueMap);

    /**
     * 向 ZSet 添加单个成员
     *
     * @param key    zset key
     * @param score  分数
     * @param member 成员
     * @return 添加成功的数量
     */
    long zadd(String key, double score, String member);

    /**
     * 获取指定范围内的成员（升序）
     *
     * @param key   zset key
     * @param start 开始下标
     * @param stop  结束下标
     * @return 成员集合
     */
    Set<String> zrange(String key, long start, long stop);

    /**
     * 获取指定范围内的成员（降序）
     *
     * @param key   zset key
     * @param start 开始下标
     * @param stop  结束下标
     * @return 成员集合
     */
    Set<String> zrevrange(String key, long start, long stop);

    /**
     * 删除 score 范围内的成员
     *
     * @param key      zset key
     * @param scoreMin 最小分
     * @param scoreMax 最大分
     * @return 删除数量
     */
    long zremrangeByScore(String key, double scoreMin, double scoreMax);

    /**
     * 获取成员的 score 值
     *
     * @param key    zset key
     * @param member 成员
     * @return 分数
     */
    Double zscore(String key, String member);

    /**
     * 为成员的 score 增加增量
     *
     * @param key       zset key
     * @param increment 增量值（可为负）
     * @param member    成员
     * @return 新的 score
     */
    Double zincrby(String key, double increment, String member);


    /** ------------------ 过期时间控制 ------------------ **/

    /**
     * 设置 key 的过期时间（秒）
     *
     * @param key     key
     * @param seconds 过期时间（秒）
     * @return 设置结果
     */
    long expire(String key, int seconds);

    /**
     * 获取 key 的剩余生存时间（秒）
     * -2：key 不存在，-1：key 永久有效
     *
     * @param key key
     * @return 秒
     */
    long ttl(String key);


    /** ------------------ List 操作 ------------------ **/

    /**
     * 从左侧插入一个或多个元素
     *
     * @param key    list key
     * @param values 元素
     * @return 插入数量
     */
    Long lpush(String key, String... values);

    /**
     * 从右侧弹出一个元素
     *
     * @param key list key
     * @return 弹出的值
     */
    String rpop(String key);
}
