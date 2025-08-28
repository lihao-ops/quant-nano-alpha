package com.hao.datacollector.integration.redis;

import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * 通用 Redis 客户端操作接口
 * 支持 String、Hash、List、ZSet 等基础 Redis 数据结构的常用命令封装
 * 提供统一的Redis操作抽象，方便后续扩展和测试
 *
 * @param <T> 泛型，主要用于String类型的操作
 * @author hli
 * @version 1.0
 * @since 2025-08-07
 */
public interface RedisClient<T> {

    /** ------------------ String 操作 ------------------ **/

    /**
     * 保存数据到 Redis，并设置失效时间（秒）
     *
     * @param key        Redis键，不能为null
     * @param value      值，不能为null
     * @param expireTime 失效时间（单位：秒），必须大于0
     * @throws IllegalArgumentException 当参数不合法时抛出
     */
    void set(String key, T value, int expireTime);

    /**
     * 保存数据到 Redis（无过期时间，永久有效）
     *
     * @param key   Redis键，不能为null
     * @param value 值，不能为null
     * @throws IllegalArgumentException 当参数不合法时抛出
     */
    void set(String key, T value);

    /**
     * 获取指定 key 的值
     *
     * @param key Redis键，不能为null
     * @return 对应的值，如果key不存在则返回null
     * @throws IllegalArgumentException 当key为null时抛出
     */
    T get(String key);

    /**
     * 检查key是否存在
     *
     * @param key Redis键，不能为null
     * @return true存在，false不存在
     * @throws IllegalArgumentException 当key为null时抛出
     */
    Boolean exists(String key);

    /**
     * 自增 1
     *
     * @param key Redis键，不能为null
     * @return 增加后的值
     * @throws IllegalArgumentException 当key为null时抛出
     */
    Long incr(String key);

    /**
     * 按指定增量自增
     *
     * @param key   Redis键，不能为null
     * @param delta 增量值
     * @return 增加后的值
     * @throws IllegalArgumentException 当key为null时抛出
     */
    Long incrBy(String key, long delta);

    /**
     * 自减 1
     *
     * @param key Redis键，不能为null
     * @return 减少后的值
     * @throws IllegalArgumentException 当key为null时抛出
     */
    Long decr(String key);

    /**
     * 按指定减量自减
     *
     * @param key   Redis键，不能为null
     * @param delta 减量值
     * @return 减少后的值
     * @throws IllegalArgumentException 当key为null时抛出
     */
    Long decrBy(String key, long delta);

    /**
     * 删除指定 key
     *
     * @param key Redis键，不能为null
     * @return 成功删除的个数（0或1）
     * @throws IllegalArgumentException 当key为null时抛出
     */
    Long del(String key);

    /**
     * 批量删除多个 key
     *
     * @param keys key 数组，不能为null或空数组
     * @return 成功删除的个数
     * @throws IllegalArgumentException 当keys为null或空时抛出
     */
    Long del(String... keys);

    /** ------------------ Hash 操作 ------------------ **/

    /**
     * 设置 Hash 值（key -> field -> value）
     *
     * @param key   hash key，不能为null
     * @param field hash field，不能为null
     * @param value hash value，不能为null
     * @throws IllegalArgumentException 当参数为null时抛出
     */
    void hset(String key, String field, String value);

    /**
     * 当且仅当 field 不存在时设置 hash 值
     *
     * @param key   hash key，不能为null
     * @param field hash field，不能为null
     * @param value hash value，不能为null
     * @return true设置成功，false字段已存在
     * @throws IllegalArgumentException 当参数为null时抛出
     */
    Boolean hsetnx(String key, String field, String value);

    /**
     * 获取 hash 中 field 对应的值
     *
     * @param key   hash key，不能为null
     * @param field hash field，不能为null
     * @return 对应值，如果field不存在则返回null
     * @throws IllegalArgumentException 当参数为null时抛出
     */
    String hget(String key, String field);

    /**
     * 获取指定 hash 的全部字段和值
     *
     * @param key hash key，不能为null
     * @return field->value的映射，如果hash不存在则返回空map
     * @throws IllegalArgumentException 当key为null时抛出
     */
    Map<String, String> hgetAll(String key);

    /**
     * 批量设置 hash 多个字段
     *
     * @param key      hash key，不能为null
     * @param paramMap field -> value 映射，不能为null或空
     * @throws IllegalArgumentException 当参数为null或空时抛出
     */
    void hmset(String key, Map<String, String> paramMap);

    /**
     * 批量获取多个字段的值
     *
     * @param key    hash key，不能为null
     * @param fields field 数组，不能为null或空
     * @return 值集合，对应fields的顺序，不存在的field对应null
     * @throws IllegalArgumentException 当参数为null或空时抛出
     */
    List<String> hmget(String key, String... fields);

    /**
     * 获取指定 hash 的所有字段名
     *
     * @param key hash key，不能为null
     * @return 所有字段名集合，如果hash不存在则返回空集合
     * @throws IllegalArgumentException 当key为null时抛出
     */
    Set<String> hkeys(String key);

    /**
     * 获取指定 hash 的所有值
     *
     * @param key hash key，不能为null
     * @return 所有值的集合，如果hash不存在则返回空集合
     * @throws IllegalArgumentException 当key为null时抛出
     */
    List<String> hvals(String key);

    /**
     * 获取hash的字段数量
     *
     * @param key hash key，不能为null
     * @return 字段数量
     * @throws IllegalArgumentException 当key为null时抛出
     */
    Long hlen(String key);

    /**
     * 检查hash中字段是否存在
     *
     * @param key   hash key，不能为null
     * @param field 字段名，不能为null
     * @return true存在，false不存在
     * @throws IllegalArgumentException 当参数为null时抛出
     */
    Boolean hexists(String key, String field);

    /**
     * 删除 hash 中一个或多个字段
     *
     * @param key    hash key，不能为null
     * @param fields 字段数组，不能为null或空
     * @return 删除数量
     * @throws IllegalArgumentException 当参数为null或空时抛出
     */
    Long hdel(String key, String... fields);

    /** ------------------ ZSet 操作 ------------------ **/

    /**
     * 添加多个成员到 ZSet，有序集合
     *
     * @param key      zset key，不能为null
     * @param valueMap member -> score 映射，不能为null或空
     * @return 添加成功的个数
     * @throws IllegalArgumentException 当参数为null或空时抛出
     */
    Long zadd(String key, Map<String, Double> valueMap);

    /**
     * 向 ZSet 添加单个成员
     *
     * @param key    zset key，不能为null
     * @param score  分数
     * @param member 成员，不能为null
     * @return 添加成功的数量（0或1）
     * @throws IllegalArgumentException 当参数为null时抛出
     */
    Long zadd(String key, double score, String member);

    /**
     * 获取指定范围内的成员（升序，按分数从小到大）
     *
     * @param key   zset key，不能为null
     * @param start 开始下标（包含）
     * @param stop  结束下标（包含），-1表示最后一个
     * @return 成员集合（有序），如果key不存在则返回空集合
     * @throws IllegalArgumentException 当key为null时抛出
     */
    Set<String> zrange(String key, long start, long stop);

    /**
     * 获取指定范围内的成员（降序，按分数从大到小）
     *
     * @param key   zset key，不能为null
     * @param start 开始下标（包含）
     * @param stop  结束下标（包含），-1表示最后一个
     * @return 成员集合（有序），如果key不存在则返回空集合
     * @throws IllegalArgumentException 当key为null时抛出
     */
    Set<String> zrevrange(String key, long start, long stop);

    /**
     * 删除 score 范围内的成员
     *
     * @param key      zset key，不能为null
     * @param scoreMin 最小分数（包含）
     * @param scoreMax 最大分数（包含）
     * @return 删除数量
     * @throws IllegalArgumentException 当key为null时抛出
     */
    Long zremrangeByScore(String key, double scoreMin, double scoreMax);

    /**
     * 删除指定成员
     *
     * @param key     zset key，不能为null
     * @param members 要删除的成员，不能为null或空
     * @return 删除数量
     * @throws IllegalArgumentException 当参数为null或空时抛出
     */
    Long zrem(String key, String... members);

    /**
     * 获取成员的 score 值
     *
     * @param key    zset key，不能为null
     * @param member 成员，不能为null
     * @return 分数，如果成员不存在则返回null
     * @throws IllegalArgumentException 当参数为null时抛出
     */
    Double zscore(String key, String member);

    /**
     * 为成员的 score 增加增量
     *
     * @param key       zset key，不能为null
     * @param increment 增量值（可为负）
     * @param member    成员，不能为null
     * @return 新的 score
     * @throws IllegalArgumentException 当参数为null时抛出
     */
    Double zincrby(String key, double increment, String member);

    /**
     * 获取有序集合的成员数量
     *
     * @param key zset key，不能为null
     * @return 成员数量
     * @throws IllegalArgumentException 当key为null时抛出
     */
    Long zcard(String key);

    /**
     * 获取指定分数范围内的成员数量
     *
     * @param key      zset key，不能为null
     * @param scoreMin 最小分数（包含）
     * @param scoreMax 最大分数（包含）
     * @return 成员数量
     * @throws IllegalArgumentException 当key为null时抛出
     */
    Long zcount(String key, double scoreMin, double scoreMax);

    /** ------------------ List 操作 ------------------ **/

    /**
     * 从左侧插入一个或多个元素
     *
     * @param key    list key，不能为null
     * @param values 元素数组，不能为null或空
     * @return 插入后列表的长度
     * @throws IllegalArgumentException 当参数为null或空时抛出
     */
    Long lpush(String key, String... values);

    /**
     * 从右侧插入一个或多个元素
     *
     * @param key    list key，不能为null
     * @param values 元素数组，不能为null或空
     * @return 插入后列表的长度
     * @throws IllegalArgumentException 当参数为null或空时抛出
     */
    Long rpush(String key, String... values);

    /**
     * 从左侧弹出一个元素
     *
     * @param key list key，不能为null
     * @return 弹出的值，如果列表为空则返回null
     * @throws IllegalArgumentException 当key为null时抛出
     */
    String lpop(String key);

    /**
     * 从右侧弹出一个元素
     *
     * @param key list key，不能为null
     * @return 弹出的值，如果列表为空则返回null
     * @throws IllegalArgumentException 当key为null时抛出
     */
    String rpop(String key);

    /**
     * 获取列表指定范围内的元素
     *
     * @param key   list key，不能为null
     * @param start 开始下标（包含）
     * @param stop  结束下标（包含），-1表示最后一个
     * @return 元素列表，如果key不存在则返回空列表
     * @throws IllegalArgumentException 当key为null时抛出
     */
    List<String> lrange(String key, long start, long stop);

    /**
     * 获取列表长度
     *
     * @param key list key，不能为null
     * @return 列表长度
     * @throws IllegalArgumentException 当key为null时抛出
     */
    Long llen(String key);

    /** ------------------ Set 操作 ------------------ **/

    /**
     * 向集合添加一个或多个成员
     *
     * @param key     set key，不能为null
     * @param members 成员数组，不能为null或空
     * @return 添加成功的数量
     * @throws IllegalArgumentException 当参数为null或空时抛出
     */
    Long sadd(String key, String... members);

    /**
     * 移除集合中的一个或多个成员
     *
     * @param key     set key，不能为null
     * @param members 成员数组，不能为null或空
     * @return 移除成功的数量
     * @throws IllegalArgumentException 当参数为null或空时抛出
     */
    Long srem(String key, String... members);

    /**
     * 获取集合的所有成员
     *
     * @param key set key，不能为null
     * @return 成员集合，如果key不存在则返回空集合
     * @throws IllegalArgumentException 当key为null时抛出
     */
    Set<String> smembers(String key);

    /**
     * 判断成员是否在集合中
     *
     * @param key    set key，不能为null
     * @param member 成员，不能为null
     * @return true存在，false不存在
     * @throws IllegalArgumentException 当参数为null时抛出
     */
    Boolean sismember(String key, String member);

    /**
     * 获取集合的成员数量
     *
     * @param key set key，不能为null
     * @return 成员数量
     * @throws IllegalArgumentException 当key为null时抛出
     */
    Long scard(String key);

    /** ------------------ 过期时间控制 ------------------ **/

    /**
     * 设置 key 的过期时间（秒）
     *
     * @param key     Redis键，不能为null
     * @param seconds 过期时间（秒），必须大于0
     * @return 设置结果，true成功，false失败
     * @throws IllegalArgumentException 当参数不合法时抛出
     */
    Boolean expire(String key, int seconds);

    /**
     * 设置 key 在指定时间戳过期
     *
     * @param key       Redis键，不能为null
     * @param timestamp Unix时间戳（秒）
     * @return 设置结果，true成功，false失败
     * @throws IllegalArgumentException 当参数不合法时抛出
     */
    Boolean expireAt(String key, long timestamp);

    /**
     * 移除key的过期时间，使其永久有效
     *
     * @param key Redis键，不能为null
     * @return true成功，false key不存在或没有设置过期时间
     * @throws IllegalArgumentException 当key为null时抛出
     */
    Boolean persist(String key);

    /**
     * 获取 key 的剩余生存时间（秒）
     * -2：key 不存在
     * -1：key 永久有效
     * >0：剩余秒数
     *
     * @param key Redis键，不能为null
     * @return 剩余生存时间（秒）
     * @throws IllegalArgumentException 当key为null时抛出
     */
    Long ttl(String key);

    /** ------------------ 通用操作 ------------------ **/

    /**
     * 获取key的数据类型
     *
     * @param key Redis键，不能为null
     * @return 数据类型字符串（string, list, set, zset, hash, none）
     * @throws IllegalArgumentException 当key为null时抛出
     */
    String type(String key);

    /**
     * 重命名key
     *
     * @param oldKey 原key，不能为null
     * @param newKey 新key，不能为null
     * @throws IllegalArgumentException 当参数为null时抛出
     */
    void rename(String oldKey, String newKey);

    /**
     * 当新key不存在时重命名key
     *
     * @param oldKey 原key，不能为null
     * @param newKey 新key，不能为null
     * @return true重命名成功，false新key已存在
     * @throws IllegalArgumentException 当参数为null时抛出
     */
    Boolean renamenx(String oldKey, String newKey);
}