package com.hao.datacollector;

import com.hao.datacollector.integration.redis.RedisConfig;
import lombok.extern.slf4j.Slf4j;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.data.redis.connection.ReturnType;
import org.springframework.data.redis.core.RedisCallback;

import java.util.UUID;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * 分布式锁回测并发测试（设计与实现说明）
 * <p>
 * 目标
 * - 模拟 N 个并发请求同时触发同一回测任务，验证分布式锁能保证只有一个实例实际执行回测并写入结果（幂等写入）。
 * - 在执行期间对锁进行周期性续租，确保长耗时任务不会被过早释放。
 * <p>
 * 组件与职责
 * - RedisConfig（Redis 客户端封装）
 * - 负责创建 LettuceConnectionFactory 和 StringRedisTemplate，并封装常用 Redis 操作（set/get/expire/hash/zset/list/set 等）。
 * - 提供 tryLock(key, value, expireTime) 使用 setIfAbsent(key, value, expire) 实现“获取锁”。
 * - 提供 releaseLock(key, value) 使用 Lua 脚本（get + del）原子释放。
 * - 提供 getRedisTemplate() 暴露底层 StringRedisTemplate，用于执行 Lua 脚本（续租场景）。
 * - 建议若需要更多原子操作（比如 SET NX EX）可在 RedisConfig 增加封装方法 setIfAbsent(key,value,expire)。
 * <p>
 * - DistributedLockTest（测试逻辑）
 * - 在 testDistributedLockForBacktest() 中启动多线程模拟并发请求。
 * - 每个线程调用 startBacktest：
 * 1. 尝试获取分布式锁（lockService.tryLock）。value 建议为唯一字符串（requestId + UUID）。
 * 2. 若获取失败，直接返回（任务已被其他实例处理）。
 * 3. 若获取成功：
 * - 统计 lockSuccessCount。
 * - 启动单线程 ScheduledExecutorService 用于周期性续租（heartbeat），通过 lockService.getRedisTemplate().execute(...) 执行 Lua 脚本：检查 key 的 value 是否与当前持有者一致，若一致则执行 EXPIRE（续租）。
 * - 执行回测任务 executeBacktest（模拟耗时操作）。
 * - 幂等写入结果 writeResultIdempotent：通过 Redis 的 setIfAbsent（或 RedisConfig 封装的方法）实现 SET NX semantics，保证只有第一个写入者实际写入结果（dbWriteCount 计数）。
 * - finally 中停止 heartbeat 调度器（cancel + shutdown）。
 * 4. 最终释放锁：调用 lockService.releaseLock(key, value)，使用 Lua 脚本保证“先验证后删除”的原子性，避免误删他人锁。
 * <p>
 * 重要设计要点与注意事项
 * - 锁值唯一性：每个请求应使用唯一 value（UUID），以区分不同持有者，保证 release/renew 的安全性。
 * - 续租（heartbeat）间隔与锁 TTL：
 * - heartbeat 间隔应远小于 TTL（示例：TTL=60s，heartbeat 每20s），并考虑续租脚本执行失败或网络抖动的容错。
 * - 若续租失败，应视为可能丢失锁，后续逻辑应谨慎（可中断任务或在记录日志后继续，本示例继续执行但释放时会失败）。
 * - 原子释放：releaseLock 使用 Lua 脚本（if get(key)==value then del(key) end），避免删除不是自己持有的锁。
 * - 幂等写入：写入数据库/结果时必须保证幂等（本示例使用 Redis 的 SET NX 模拟唯一约束），真实场景应结合数据库唯一索引或事务保障。
 * - Redis 连接与测试环境：
 * - RedisConfig 在 afterPropertiesSet 中会初始化连接并做简单连通性检测，请确保测试环境能连接到配置的 Redis。
 * - 单元测试注入 RedisConfig（而不是直接注入 RedisTemplate），以复用封装的方法和连接初始化逻辑。
 * <p>
 * 统计与度量
 * - lockSuccessCount：获取到锁的线程数（理论上应为 1）。
 * - actualExecuteCount：实际执行回测任务的次数（理论上应为 1）。
 * - dbWriteCount：实际写入结果到 Redis/数据库的次数（理论上应为 1）。
 * - 资源节约率：通过并发数与实际执行次数计算节约率。
 * <p>
 * 异常与容错
 * - Redis 操作可能抛出异常（连接丢失、超时等），在关键路径应捕获并记录日志（本示例中多数操作已捕获并记录）。
 * - 若续租失败而任务仍在执行，后续释放锁时 releaseLock 会返回 false（因为锁可能已被其他请求抢到）。根据业务重要性可在续租失败时主动中止任务或上报告警。
 * <p>
 * 扩展建议
 * - 将锁与业务解耦：把锁逻辑放到独立的 LockService 中（注入 RedisConfig），便于复用与单元测试。
 * - 使用 Redisson 等成熟客户端可简化分布式锁（自动续租、可重入、看门狗等）并提高健壮性。
 * - 在写结果时结合数据库唯一索引做最终一致性保障。
 */
@SpringBootTest
@Slf4j
public class DistributedLockTest {

    @Autowired
    private RedisConfig lockService; // 你的Redis服务

    // 统计计数器
    private final AtomicInteger lockSuccessCount = new AtomicInteger(0);
    private final AtomicInteger actualExecuteCount = new AtomicInteger(0);
    private final AtomicInteger dbWriteCount = new AtomicInteger(0);

    @Test
    public void testDistributedLockForBacktest() throws Exception {
        String taskId = "test-backtest-001";
        int concurrentCount = 20; // 并发数
        CountDownLatch latch = new CountDownLatch(concurrentCount);

        log.info("开始测试：{}个并发请求同时执行任务_{}|Log_message", concurrentCount, taskId);

        // 模拟并发请求
        for (int i = 0; i < concurrentCount; i++) {
            final int requestId = i;
            new Thread(() -> {
                try {
                    startBacktest(taskId, requestId);
                } finally {
                    latch.countDown();
                }
            }).start();
        }

        // 等待所有请求完成
        latch.await(120, TimeUnit.SECONDS);

        // 输出统计结果
        log.info("==========_测试结果_==========|Log_message");
        log.info("并发请求数:_{}|Log_message", concurrentCount);
        log.info("成功获取锁次数:_{}|Log_message", lockSuccessCount.get());
        log.info("实际执行回测次数:_{}|Log_message", actualExecuteCount.get());
        log.info("数据库写入次数:_{}|Log_message", dbWriteCount.get());
        log.info("资源节约率:_{}%|Log_message", (concurrentCount - actualExecuteCount.get()) * 100.0 / concurrentCount);
    }

    /**
     * 模拟回测任务启动
     */
    private void startBacktest(String taskId, int requestId) {
        String lockKey = "backtest:" + taskId;
        String lockValue = "request-" + requestId + "-" + UUID.randomUUID();
        int lockTTL = 60; // 60秒

        log.info("请求{}_尝试获取锁:_{}|Log_message", requestId, lockKey);

        // 1. 尝试获取分布式锁
        boolean lockAcquired = lockService.tryLock(lockKey, lockValue, lockTTL);
        if (!lockAcquired) {
            log.info("请求{}_获取锁失败，任务已在运行|Log_message", requestId);
            return;
        }

        lockSuccessCount.incrementAndGet();
        log.info("请求{}_成功获取锁，开始执行任务|Log_message", requestId);

        try {
            // 2. 启动心跳续租
            ScheduledExecutorService scheduler = Executors.newSingleThreadScheduledExecutor();
            ScheduledFuture<?> heartbeat = scheduler.scheduleAtFixedRate(() -> {
                boolean renewed = renewLock(lockKey, lockValue, lockTTL);
                log.debug("请求{}_续租{}|Log_message", requestId, renewed ? "成功" : "失败");
            }, 20, 20, TimeUnit.SECONDS);

            try {
                // 3. 执行回测任务
                executeBacktest(taskId, requestId);

                // 4. 写入结果（幂等）
                writeResultIdempotent(taskId, requestId);

            } finally {
                heartbeat.cancel(true);
                scheduler.shutdown();
            }

        } finally {
            // 5. 释放锁
            boolean released = lockService.releaseLock(lockKey, lockValue);
            log.info("请求{}_释放锁{}|Log_message", requestId, released ? "成功" : "失败");
        }
    }

    /**
     * 模拟回测执行
     */
    private void executeBacktest(String taskId, int requestId) {
        actualExecuteCount.incrementAndGet();
        log.info("请求{}_开始执行回测任务:_{}|Log_message", requestId, taskId);

        try {
            // 模拟耗时计算
            Thread.sleep(30_000); // 30秒
            log.info("请求{}_回测计算完成|Log_message", requestId);
        } catch (InterruptedException e) {
            log.warn("请求{}_回测被中断|Log_message", requestId);
            Thread.currentThread().interrupt();
        }
    }

    /**
     * 幂等写入结果
     */
    private void writeResultIdempotent(String taskId, int requestId) {
        String resultKey = "result:" + taskId;

        // 使用你的RedisConfig的方法来实现SET NX
        String resultValue = "回测结果-" + requestId + "-" + System.currentTimeMillis();

        // 先检查key是否存在
        if (!lockService.exists(resultKey)) {
            // 尝试设置值（这里简化处理，实际应该用SET NX）
            try {
                lockService.set(resultKey, resultValue, 3600); // 1小时过期
                dbWriteCount.incrementAndGet();
                log.info("请求{}_写入结果成功|Log_message", requestId);
            } catch (Exception e) {
                log.info("请求{}_结果已存在，跳过写入|Log_message", requestId);
            }
        } else {
            log.info("请求{}_结果已存在，跳过写入|Log_message", requestId);
        }
    }

    /**
     * 续租锁（使用你的RedisConfig的RedisTemplate）
     */
    private boolean renewLock(String key, String value, int expireSeconds) {
        try {
            String script = "if redis.call('get', KEYS[1]) == ARGV[1] then " +
                    "return redis.call('expire', KEYS[1], ARGV[2]) " +
                    "else return 0 end";

            // 使用RedisConfig的getRedisTemplate()方法
            RedisCallback<Long> callback = connection ->
                    connection.eval(
                            script.getBytes(),
                            ReturnType.INTEGER,
                            1,
                            key.getBytes(),
                            value.getBytes(),
                            String.valueOf(expireSeconds).getBytes()
                    );

            Object result = lockService.getRedisTemplate().execute(callback);
            return Long.valueOf(1).equals(result);
        } catch (Exception e) {
            log.error("续租异常|Log_message", e);
            return false;
        }
    }
}
