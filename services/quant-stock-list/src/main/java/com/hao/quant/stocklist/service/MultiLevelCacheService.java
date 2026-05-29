package com.hao.quant.stocklist.service;

import com.alibaba.csp.sentinel.Entry;
import com.alibaba.csp.sentinel.SphU;
import com.alibaba.csp.sentinel.slots.block.BlockException;
import com.alibaba.csp.sentinel.slots.block.RuleConstant;
import com.alibaba.csp.sentinel.slots.block.flow.FlowRule;
import com.alibaba.csp.sentinel.slots.block.flow.FlowRuleManager;
import com.github.benmanes.caffeine.cache.Cache;
import com.hao.quant.stocklist.mapper.StockSignalMapper;
import com.hao.quant.stocklist.model.StockSignal;
import constants.RedisKeyConstants;
import constants.SentinelResourceConstants;
import jakarta.annotation.PostConstruct;
import lombok.extern.slf4j.Slf4j;
import org.redisson.api.RList;
import org.redisson.api.RLock;
import org.redisson.api.RedissonClient;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Service;
import util.JsonUtil;

import java.time.Duration;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

/**
 * 多级缓存服务 (Multi-Level Cache Service)
 * <p>
 * 三层保护的高并发设计：
 * L1 Caffeine（3s TTL）-> L2 Redis（24h）-> L3 MySQL（兜底）
 * <p>
 * 并发保护机制：
 * 1. L1->L2: 利用 Caffeine 的 {@code get(key, loader)} 内置并发控制，
 *    同一 key 只有 1 个线程执行 loader，其他线程自动等待结果（非阻塞式）。
 *    相比旧方案（Redis 分布式锁 + Thread.sleep），优势：
 *    - 不阻塞 Web 线程，万级并发下不会耗尽线程池
 *    - 等待线程拿到的是加载完成后的真实数据，不会误返回空列表
 *    - 无分布式锁网络开销，性能更高
 * 2. L2->L3: Redis 分布式锁（跨 Pod 保护） + Sentinel 限流（QPS=1），
 *    多 Pod 部署时只有 1 个 Pod 能查 MySQL，避免数据库被打爆
 * 3. 空值保护: EMPTY_MARKER 特殊标记（10s TTL），查无数据时缓存标记，避免重复穿透
 *
 * @author hli
 * @date 2026-01-30
 */
@Slf4j
@Service
public class MultiLevelCacheService {

    /**
     * Redis Key 前缀：使用统一常量
     */
    private static final String REDIS_KEY_PREFIX = RedisKeyConstants.STOCK_SIGNAL_LIST_PREFIX;

    /**
     * 分布式锁 Key 前缀：使用统一常量
     */
    private static final String LOCK_KEY_PREFIX = RedisKeyConstants.STOCK_SIGNAL_LOCK_PREFIX;

    /**
     * 空值标记：使用统一常量
     */
    private static final String EMPTY_MARKER = RedisKeyConstants.CACHE_EMPTY_MARKER;

    /**
     * Sentinel 资源名称：L3 数据库查询
     */
    private static final String SENTINEL_RESOURCE_L3_QUERY = SentinelResourceConstants.STOCK_LIST_L3_QUERY;

    /**
     * L3 查询分布式锁等待时间（秒），用于跨 Pod 并发保护
     */
    private static final long DB_LOCK_WAIT_TIME = 2;

    /**
     * L3 查询分布式锁自动释放时间（秒），Redisson 看门狗机制会自动续期
     * todo :注意一定是-1,否则不会生效
     */
    private static final long DB_LOCK_LEASE_TIME = -1;

    /**
     * 正常数据缓存过期时间：24 小时
     */
    private static final long CACHE_TTL_HOURS = 24;

    /**
     * 空值标记缓存过期时间：10 秒
     * <p>
     * 设为 10 秒而非更长的原因：
     * 1. 保证数据新鲜度——若数据在空值标记期间入库，最多 10 秒后即可查到
     * 2. L3 已有 Sentinel QPS=1 限流保护，即使空值频繁过期，MySQL 最多承受 1 QPS，压力可控
     * 3. L1 Caffeine 本身有 3 秒 TTL，在 10 秒窗口内可再挡住大部分重复请求
     */
    private static final long EMPTY_CACHE_TTL_SECONDS = 10;

    @Autowired
    @Qualifier("stockSignalCache")
    private Cache<String, List<String>> caffeineCache;



    @Autowired
    private RedissonClient redissonClient;

    @Autowired
    private StockSignalMapper stockSignalMapper;

    /**
     * 初始化 Sentinel 限流规则
     */
    @PostConstruct
    public void initSentinelRules() {
        List<FlowRule> rules = new ArrayList<>();
        FlowRule rule = new FlowRule();
        rule.setResource(SENTINEL_RESOURCE_L3_QUERY);
        rule.setGrade(RuleConstant.FLOW_GRADE_QPS);
        rule.setCount(1);  // QPS = 1，只允许1个请求/秒进入L3
        rule.setLimitApp("default");
        rules.add(rule);
        FlowRuleManager.loadRules(rules);
        log.info("Sentinel规则初始化完成|Sentinel_rules_loaded,resource={},qps=1", SENTINEL_RESOURCE_L3_QUERY);
    }

    /**
     * 多级缓存查询股票信号列表
     * <p>
     * 查询顺序：L1 Caffeine -> L2 Redis -> L3 MySQL
     * <p>
     * 并发控制：使用 Caffeine 的 {@code get(key, loader)} 机制，
     * 底层基于 {@code ConcurrentHashMap.computeIfAbsent}，同一 key 同一时刻只有 1 个线程执行 loader，
     * 其他线程自动等待结果返回，无需 Thread.sleep 或分布式锁。
     * <p>
     * 相比旧方案（Redis 分布式锁 + Thread.sleep(100)）的优势：
     * 1. 不阻塞 Web 线程——万级并发下不会耗尽 Tomcat 线程池
     * 2. 等待线程拿到的是真实数据——不会因为抢锁失败误返回空列表
     * 3. 纯本地操作——无分布式锁的网络 RTT 开销
     *
     * @param strategyId 策略ID
     * @param tradeDate  交易日字符串（yyyy-MM-dd）
     * @return 信号列表（JSON 字符串列表）
     */
    public List<String> querySignals(String strategyId, String tradeDate) {
        String cacheKey = buildCacheKey(strategyId, tradeDate);

        // Caffeine.get(key, loader)：同一 key 只有 1 个线程执行 loader，其他线程等待结果
        // loader 内部依次查 L2 Redis -> L3 MySQL，加载完成后自动缓存到 L1
        return caffeineCache.get(cacheKey, key -> loadFromL2OrL3(strategyId, tradeDate, key));
    }

    /**
     * 从 L2 Redis 或 L3 MySQL 加载数据（Caffeine loader 回调）
     * <p>
     * 此方法由 Caffeine 的 {@code get(key, loader)} 调用，保证同一 key 同一时刻只有 1 个线程进入
     *
     * @param strategyId 策略ID
     * @param tradeDate  交易日
     * @param cacheKey   缓存键
     * @return 信号列表
     */
    private List<String> loadFromL2OrL3(String strategyId, String tradeDate, String cacheKey) {
        // 先查 L2 Redis（无需分布式锁，本地 Caffeine 已保证单线程进入）
        List<String> signals = queryFromRedis(cacheKey);
        if (signals != null) {
            log.debug("L2缓存命中_回填L1|L2_hit_backfill_L1,key={}", cacheKey);
            return signals;
        }

        // L2 未命中，进入 L3（带分布式锁 + Sentinel 限流）
        return queryFromDbWithLock(strategyId, tradeDate, cacheKey);
    }

    /**
     * 从 Redis 查询信号列表
     * 使用 Redisson 原生 API 避免 Spring Data Redis 兼容性问题
     *
     * @param cacheKey 缓存键
     * @return 信号列表，不存在返回 null，空标记返回空列表
     */
    private List<String> queryFromRedis(String cacheKey) {
        try {
            RList<String> rlist = redissonClient.getList(cacheKey);
            List<String> signals = rlist.readAll();

            if (signals == null || signals.isEmpty()) {
                return null;
            }

            // 检查是否是空值标记
            if (signals.size() == 1 && EMPTY_MARKER.equals(signals.get(0))) {
                log.debug("L2空值标记命中|L2_empty_marker_hit,key={}", cacheKey);
                return Collections.emptyList();
            }

            log.debug("L2缓存命中|L2_cache_hit,key={},size={}", cacheKey, signals.size());
            return signals;
        } catch (Exception e) {
            log.warn("Redis查询失败|Redis_query_failed,key={},error={}", cacheKey, e.getMessage());
            return null;
        }
    }

    /**
     * 从 MySQL 查询信号列表（带分布式锁 + Sentinel 限流双重保护）
     * <p>
     * 分布式锁：跨 Pod 保护，多 Pod 部署时只有 1 个 Pod 能执行数据库查询，
     * 其他 Pod 等锁释放后直接读 Redis（获胜 Pod 已回填），避免 DB 被多 Pod 同时打爆。
     * <p>
     * Sentinel 限流：兜底保护，QPS=1 防止单 Pod 内异常场景（如锁故障）导致大量请求穿透到数据库。
     *
     * @param strategyId 策略ID
     * @param tradeDate  交易日
     * @param cacheKey   缓存键
     * @return 信号列表
     */
    private List<String> queryFromDbWithLock(String strategyId, String tradeDate, String cacheKey) {
        String lockKey = LOCK_KEY_PREFIX + "db:" + strategyId + ":" + tradeDate;
        RLock lock = redissonClient.getLock(lockKey);

        boolean locked = false;
        try {
            // 尝试获取 Redis 分布式锁 (Distributed Lock)
            // 参数 1 (waitTime): DB_LOCK_WAIT_TIME -> 抢锁的最大等待时间。若超过此时间仍被其他线程占用，则直接返回 false，触发快速失败 (Fast Fail)。
            // 参数 2 (leaseTime): -1 -> 核心架构设定！将租期 (Lease Time) 设为 -1，显式开启 Redisson 的看门狗机制 (Watchdog Mechanism)。
            //    -> 底层行为: 只要当前线程未调用 unlock() 且 JVM 进程存活，底层的 Netty 定时任务会默认每 10 秒自动为该锁续期 (Renew) 至 30 秒。
            //    -> 适用场景: 确保耗时不确定的长任务能够完整执行，绝对避免业务执行中途锁超时释放导致的并发安全撕裂问题。
            // 参数 3 (unit): TimeUnit.SECONDS -> 统一时间单位为秒。
            locked = lock.tryLock(DB_LOCK_WAIT_TIME, -1, TimeUnit.SECONDS);

            if (locked) {
                // 双重检查：获取锁后先查 Redis，可能其他 Pod 已经回填
                List<String> signals = queryFromRedis(cacheKey);
                if (signals != null) {
                    log.debug("获取锁后L2命中|L2_hit_after_lock,key={}", cacheKey);
                    return signals;
                }

                // Redis 无数据，执行数据库查询（Sentinel 限流保护）
                return queryFromDbWithSentinel(strategyId, tradeDate, cacheKey);
            } else {
                // 未获取到锁（其他 Pod 正在查 DB），等待后读 Redis
                log.debug("L3锁竞争_等待Redis回填|L3_lock_contention,key={}", cacheKey);
                List<String> signals = queryFromRedis(cacheKey);
                return signals != null ? signals : Collections.emptyList();
            }
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            log.warn("获取L3锁被中断|L3_lock_interrupted,key={}", cacheKey);
            return Collections.emptyList();
        } catch (Exception e) {
            // 🚀 新增修改点：在这里接住底层抛出的 SocketTimeoutException 等运行时异常
            // 只要走到这里，代码就会顺理成章地进入下面的 finally 释放锁，看门狗自动销毁！
            log.error("L3查询遭遇异常(可能是网络假死触发了底层的 socketTimeout)|L3_query_error, key={}", cacheKey, e);
            return Collections.emptyList();
        } finally {
            if (locked && lock.isHeldByCurrentThread()) {
                lock.unlock();
            }
        }
    }

    /**
     * 从 MySQL 查询信号列表（Sentinel 限流保护）
     * <p>
     * Sentinel 限流 QPS=1，作为最后一道防线防止数据库被打爆
     */
    private List<String> queryFromDbWithSentinel(String strategyId, String tradeDate, String cacheKey) {
        Entry entry = null;
        try {
            // Sentinel 限流入口
            entry = SphU.entry(SENTINEL_RESOURCE_L3_QUERY);

            // 查询 MySQL
            log.info("L3数据库查询|L3_db_query,strategy={},date={}", strategyId, tradeDate);
            List<StockSignal> dbSignals = stockSignalMapper.selectPassedSignals(strategyId, tradeDate);

            List<String> signals;
            if (dbSignals == null || dbSignals.isEmpty()) {
                // 查无数据，缓存空值标记
                signals = Collections.emptyList();
                cacheEmptyMarker(cacheKey);
            } else {
                // 转换为 JSON 列表
                signals = dbSignals.stream()
                        .map(JsonUtil::toJson)
                        .collect(Collectors.toList());
                // 回填 Redis
                cacheToRedis(cacheKey, signals);
            }

            return signals;

        } catch (BlockException e) {
            // 被限流，返回空列表（不穿透到数据库）
            log.warn("L3被限流|L3_blocked_by_sentinel,strategy={},date={}", strategyId, tradeDate);
            return Collections.emptyList();
        } finally {
            if (entry != null) {
                entry.exit();
            }
        }
    }

    /**
     * 缓存数据到 Redis
     * 使用 Redisson 原生 API 避免 Spring Data Redis 兼容性问题
     */
    private void cacheToRedis(String cacheKey, List<String> signals) {
        try {
            RList<String> rlist = redissonClient.getList(cacheKey);
            rlist.addAll(signals);
            rlist.expire(Duration.ofHours(CACHE_TTL_HOURS));
            log.info("L3结果回填Redis|L3_result_cached,key={},size={}", cacheKey, signals.size());
        } catch (Exception e) {
            log.warn("Redis回填失败|Redis_cache_refill_failed,key={}", cacheKey, e);
        }
    }

    /**
     * 缓存空值标记到 Redis（防止缓存击穿）
     * 使用 Redisson 原生 API 避免 Spring Data Redis 兼容性问题
     */
    private void cacheEmptyMarker(String cacheKey) {
        try {
            RList<String> rlist = redissonClient.getList(cacheKey);
            rlist.add(EMPTY_MARKER);
            rlist.expire(Duration.ofSeconds(EMPTY_CACHE_TTL_SECONDS));
            log.info("空值标记已缓存|Empty_marker_cached,key={},ttl={}s", cacheKey, EMPTY_CACHE_TTL_SECONDS);
        } catch (Exception e) {
            log.warn("空值标记缓存失败|Empty_marker_cache_failed,key={}", cacheKey, e);
        }
    }

    /**
     * 构建缓存键
     */
    private String buildCacheKey(String strategyId, String tradeDate) {
        return REDIS_KEY_PREFIX + strategyId + ":" + tradeDate;
    }

    /**
     * 手动刷新本地缓存（供测试使用）
     */
    public void invalidateLocalCache(String strategyId, String tradeDate) {
        String cacheKey = buildCacheKey(strategyId, tradeDate);
        caffeineCache.invalidate(cacheKey);
        log.info("本地缓存已失效|Local_cache_invalidated,key={}", cacheKey);
    }

    /**
     * 清理 Redis 缓存（供测试使用，防止脏数据）
     * 使用 Redisson 原生 API 避免与 Spring Data Redis 的兼容性问题
     *
     * @param strategyId 策略ID
     * @param tradeDate  交易日
     */
    public void invalidateRedisCache(String strategyId, String tradeDate) {
        String cacheKey = buildCacheKey(strategyId, tradeDate);
        try {
            // 使用 Redisson 原生 API 删除 key，避免 Spring Data Redis 的 pExpire 递归问题
            boolean deleted = redissonClient.getBucket(cacheKey).delete();
            log.info("Redis缓存已清理|Redis_cache_invalidated,key={},deleted={}", cacheKey, deleted);
        } catch (Exception e) {
            log.warn("Redis缓存清理失败|Redis_cache_invalidate_failed,key={}", cacheKey, e);
        }
    }

    /**
     * 清理所有缓存（本地 + Redis）
     */
    public void invalidateAllCache(String strategyId, String tradeDate) {
        invalidateLocalCache(strategyId, tradeDate);
        invalidateRedisCache(strategyId, tradeDate);
    }
}
