package com.hao.strategyengine.chain;

import com.google.common.util.concurrent.RateLimiter;
import com.hao.strategyengine.common.model.core.StrategyContext;
import com.hao.strategyengine.core.StrategyHandler;
import com.hao.strategyengine.monitoring.RateLimitMetrics;
import enums.strategy.StrategyMetaEnum;
import jakarta.annotation.PostConstruct;
import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.script.DefaultRedisScript;
import org.springframework.stereotype.Component;

import java.util.Collections;
import java.util.concurrent.TimeUnit;

/**
 * @author hli
 * @program: quant-nano-alpha
 * @Date 2025-10-23 20:07:41
 * @description: 分布式限流处理器
 * <p>
 * 核心特性:
 * 1. Redis滑动窗口实现分布式限流
 * 2. 单机令牌桶作为降级方案
 * 3. 多维度限流: 全局 + 用户 + 策略类型
 */
@Slf4j
@Component
public class RateLimitHandler implements StrategyHandler {

    @Resource
    private StringRedisTemplate stringRedisTemplate;

    @Resource
    private RateLimitMetrics rateLimitMetrics;

    // ==================== 配置参数 ====================
    /**
     * 全局是否启用限流功能
     */
    @Value("${rate-limit.enabled:true}")
    private boolean rateLimitEnabled;

    /**
     * 是否启用分布式限流
     */
    @Value("${rate-limit.distributed.enabled:true}")
    private boolean distributedEnabled;

    /**
     * Redis限流key前缀
     */
    @Value("${rate-limit.redis.key-prefix:quant:rate_limit:}")
    private String redisKeyPrefix;

    /**
     * 全局QPS限制
     */
    @Value("${rate-limit.global.qps:200}")
    private int globalQps;

    /**
     * 单用户QPS限制
     */
    @Value("${rate-limit.user.qps:10}")
    private int userQps;
    /**
     * todo 改进成最大-策略类型QPS限制
     */
    /**
     * 策略类型QPS限制
     */
    @Value("${rate-limit.strategy.simple:100}")
    private int simpleStrategyQps;

    @Value("${rate-limit.strategy.complex:20}")
    private int complexStrategyQps;

    @Value("${rate-limit.strategy.ml-model:5}")
    private int mlModelStrategyQps;

    // ==================== 单机降级限流器 ====================

    /**
     * 单机全局限流器(降级用)
     */
    private RateLimiter localGlobalLimiter;

    /**
     * 单机用户限流器(降级用)
     */
    private RateLimiter localUserLimiter;

    // ==================== Lua脚本 ====================

    /**
     * Redis滑动窗口限流Lua脚本
     * <p>
     * 算法原理:
     * 1. 使用ZSET存储请求时间戳
     * 2. 移除窗口外的旧数据
     * 3. 统计窗口内请求数
     * 4. 判断是否超过限制
     * <p>
     * KEYS[1]: 限流key
     * ARGV[1]: 窗口大小(秒)
     * ARGV[2]: 限流阈值
     * ARGV[3]: 当前时间戳(毫秒)
     * <p>
     * 返回: 1=通过, 0=拒绝
     */
    private static final String RATE_LIMIT_LUA_SCRIPT =
            "local key = KEYS[1]\n" +
                    "local window = tonumber(ARGV[1])\n" +
                    "local limit = tonumber(ARGV[2])\n" +
                    "local now = tonumber(ARGV[3])\n" +
                    "\n" +
                    "-- 计算窗口起始时间\n" +
                    "local windowStart = now - window * 1000\n" +
                    "\n" +
                    "-- 移除窗口外的旧数据\n" +
                    "redis.call('ZREMRANGEBYSCORE', key, 0, windowStart)\n" +
                    "\n" +
                    "-- 统计窗口内的请求数\n" +
                    "local current = redis.call('ZCARD', key)\n" +
                    "\n" +
                    "-- 判断是否超过限制\n" +
                    "if current < limit then\n" +
                    "    -- 添加当前请求\n" +
                    "    redis.call('ZADD', key, now, now)\n" +
                    "    -- 设置key过期时间(窗口大小+1秒)\n" +
                    "    redis.call('EXPIRE', key, window + 1)\n" +
                    "    return 1\n" +
                    "else\n" +
                    "    return 0\n" +
                    "end";

    private DefaultRedisScript<Long> rateLimitScript;

    @PostConstruct
    public void init() {
        // 初始化Lua脚本
        rateLimitScript = new DefaultRedisScript<>();
        rateLimitScript.setScriptText(RATE_LIMIT_LUA_SCRIPT);
        rateLimitScript.setResultType(Long.class);

        // 初始化单机降级限流器
        localGlobalLimiter = RateLimiter.create(globalQps);
        localUserLimiter = RateLimiter.create(userQps);

        log.info("分布式限流初始化完成|Rate_limit_init_done,distributedEnabled={},globalQps={},userQps={}", distributedEnabled, globalQps, userQps);
    }

    @Override
    public void handle(StrategyContext ctx) throws Exception {
        long start = System.currentTimeMillis();
        if (!rateLimitEnabled) {
            log.info("限流关闭直接放行|Rate_limit_disabled,userId={}", ctx.getUserId());
            return;
        }
        Integer userId = ctx.getUserId();
//        String strategyType = ctx.getExtra().get("strategyType").toString();
        String strategyType = "test";
        log.info("限流检查开始|Rate_limit_check_start,userId={},strategyType={},mode={}", userId, strategyType, distributedEnabled ? "分布式" : "单机");

        try {
            // 第一层: 全局限流
            checkGlobalRateLimit();

            // 第二层: 用户维度限流
            checkUserRateLimit(userId);

            // 第三层: 策略类型限流
            checkStrategyTypeRateLimit(strategyType);

            log.info("限流检查通过|Rate_limit_check_passed,userId={},strategyType={}", userId, strategyType);
//            boolean acquired = localUserLimiter.tryAcquire(100, TimeUnit.MILLISECONDS);
            rateLimitMetrics.recordWaitTime("checkHandleRateLimit_USER", System.currentTimeMillis() - start);
        } catch (RateLimitException e) {
            log.warn("限流拒绝|Rate_limit_rejected,userId={},strategyType={},reason={}", userId, strategyType, e.getMessage());
            //监控
            rateLimitMetrics.recordRateLimitReject(e.getLimitType(), ctx.getUserId().toString(), strategyType);
            throw e;
        }
    }

    /**
     * 第一层: 全局限流
     */
    private void checkGlobalRateLimit() throws RateLimitException {
        String key = redisKeyPrefix + "global";

        if (distributedEnabled) {
            // 分布式限流
            if (!tryAcquireDistributed(key, 1, globalQps)) {
                throw new RateLimitException("GLOBAL", String.format("系统繁忙,请稍后重试 (全局QPS限制: %d)", globalQps));
            }
        } else {
            // 单机降级
            if (!localGlobalLimiter.tryAcquire(100, TimeUnit.MILLISECONDS)) {
                throw new RateLimitException("GLOBAL", String.format("系统繁忙,请稍后重试 (单机全局QPS限制: %d)", globalQps));
            }
        }
    }

    /**
     * 第二层: 用户维度限流
     */
    private void checkUserRateLimit(Integer userId) throws RateLimitException {
        long start = System.currentTimeMillis();
        String key = redisKeyPrefix + "user:" + userId;
        if (distributedEnabled) {
            // 分布式限流
            if (!tryAcquireDistributed(key, 1, userQps)) {
                throw new RateLimitException("USER", String.format("操作过于频繁,请稍后重试 (用户QPS限制: %d)", userQps));
            }
        } else {
            // 单机降级
            if (!localUserLimiter.tryAcquire(100, TimeUnit.MILLISECONDS)) {
                throw new RateLimitException("USER", String.format("操作过于频繁,请稍后重试 (单机用户QPS限制: %d)", userQps));
            }
        }
        rateLimitMetrics.recordWaitTime("checkUserRateLimit_USER", System.currentTimeMillis() - start);
    }

    /**
     * 第三层: 策略类型限流
     */
    private void checkStrategyTypeRateLimit(String strategyType) throws RateLimitException {
        // 策略类型为空则跳过
        if (strategyType == null) return;

        // 获取策略类型对应的QPS限制
        StrategyMetaEnum meta = StrategyMetaEnum.fromId(strategyType);
        if (meta == null) return; // 未配置限流则跳过

        String key = redisKeyPrefix + "strategy:" + strategyType;

        if (distributedEnabled) {
            // 分布式限流
            if (!tryAcquireDistributed(key, 1, meta.getDistributedQps())) {
                throw new RateLimitException("STRATEGY_TYPE", String.format("该策略类型执行频率过高,请稍后重试 (策略: %s, QPS限制: %d)", strategyType, meta.getDistributedQps()));
            }
        } else {
            // 单机限流(简化处理,使用用户限流器)
            if (!localUserLimiter.tryAcquire(100, TimeUnit.MILLISECONDS)) {
                throw new RateLimitException("STRATEGY_TYPE", String.format("该策略类型执行频率过高,请稍后重试 (策略: %s)", strategyType));
            }
        }
    }

    /**
     * todo 看是否要改进 - 获取策略类型对应的QPS限制
     */
    private Integer getStrategyTypeLimit(String strategyType) {
        switch (strategyType.toUpperCase()) {
            case "SIMPLE":
                return simpleStrategyQps;
            case "COMPLEX":
                return complexStrategyQps;
            case "ML_MODEL":
            case "ML-MODEL":
                return mlModelStrategyQps;
            default:
                return null;  // 未配置的策略类型不限流
        }
    }

    /**
     * 分布式限流核心方法 - 使用Redis Lua脚本
     *
     * @param key           限流key
     * @param windowSeconds 窗口大小(秒)
     * @param limit         限流阈值
     * @return true=通过, false=拒绝
     */
    private boolean tryAcquireDistributed(String key, int windowSeconds, int limit) {
        try {
            long now = System.currentTimeMillis();

            // 执行Lua脚本
            Long result = stringRedisTemplate.execute(
                    rateLimitScript,
                    Collections.singletonList(key),
                    String.valueOf(windowSeconds),
                    String.valueOf(limit),
                    String.valueOf(now)
            );

            return result != null && result == 1;

        } catch (Exception e) {
            log.error("Redis限流异常降级|Redis_rate_limit_error_fallback,key={}", key, e);
            // Redis异常时降级到单机限流
            return localGlobalLimiter.tryAcquire(100, TimeUnit.MILLISECONDS);
        }
    }

    /**
     * 限流异常
     */
    public static class RateLimitException extends Exception {
        private final String limitType;

        public RateLimitException(String limitType, String message) {
            super(message);
            this.limitType = limitType;
        }

        public String getLimitType() {
            return limitType;
        }
    }
}
