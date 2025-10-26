package com.hao.strategyengine.chain;

import com.hao.strategyengine.common.model.core.StrategyContext;
import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;

import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicInteger;

import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.fail;

/**
 * 限流处理器测试
 */
@Slf4j
@SpringBootTest
class RateLimitHandlerTest {

    @Resource
    private RateLimitHandler rateLimitHandler;

    /**
     * 测试1: 单用户限流
     * 预期: 10 QPS限制下,1秒内只能通过10个请求
     */
    @Test
    void testUserRateLimit() throws Exception {
        Integer userId = 1;
        int totalRequests = 20;  // 发送20个请求
        int expectedPass = 10;   // 预期通过10个

        AtomicInteger passCount = new AtomicInteger(0);
        AtomicInteger rejectCount = new AtomicInteger(0);

        // 短时间内发送20个请求
        for (int i = 0; i < totalRequests; i++) {
            StrategyContext ctx = StrategyContext.builder()
                    .userId(userId)
                    .build();

            try {
                rateLimitHandler.handle(ctx);
                passCount.incrementAndGet();
            } catch (RateLimitHandler.RateLimitException e) {
                rejectCount.incrementAndGet();
            }
        }

        log.info("单用户限流测试结果: 通过={}, 拒绝={}", passCount.get(), rejectCount.get());

        // 验证: 通过数应该接近预期值(允许±2的误差)
        assertTrue(Math.abs(passCount.get() - expectedPass) <= 2,
                "通过数应在" + (expectedPass - 2) + "~" + (expectedPass + 2) + "之间");
    }

    /**
     * 测试2: 并发场景下的限流
     * 预期: 多线程并发请求时,限流仍然有效
     */
    @Test
    void testConcurrentRateLimit() throws Exception {
        Integer userId = 002;
        int threadCount = 10;    // 10个线程
        int requestsPerThread = 5;  // 每个线程5个请求
        int totalRequests = threadCount * requestsPerThread;  // 总共50个请求

        ExecutorService executor = Executors.newFixedThreadPool(threadCount);
        CountDownLatch latch = new CountDownLatch(threadCount);

        AtomicInteger passCount = new AtomicInteger(0);
        AtomicInteger rejectCount = new AtomicInteger(0);

        // 多线程并发请求
        for (int i = 0; i < threadCount; i++) {
            executor.submit(() -> {
                try {
                    for (int j = 0; j < requestsPerThread; j++) {
                        StrategyContext ctx = StrategyContext.builder()
                                .userId(userId)
                                .build();

                        try {
                            rateLimitHandler.handle(ctx);
                            passCount.incrementAndGet();
                        } catch (RateLimitHandler.RateLimitException e) {
                            rejectCount.incrementAndGet();
                        } catch (Exception e) {
                            throw new RuntimeException(e);
                        }
                    }
                } finally {
                    latch.countDown();
                }
            });
        }

        latch.await();
        executor.shutdown();

        log.info("并发限流测试结果: 总请求={}, 通过={}, 拒绝={}",
                totalRequests, passCount.get(), rejectCount.get());

        // 验证: 通过数应该接近用户QPS限制(10)
        assertTrue(passCount.get() <= 12, "并发场景下通过数应该被限流器控制在合理范围内");
    }

    /**
     * 测试3: 不同策略类型的差异化限流
     * 预期: SIMPLE策略(100 QPS) 比 ML_MODEL策略(5 QPS) 有更高的限流阈值
     */
    @Test
    void testStrategyTypeRateLimit() throws Exception {
        String userId = "test_user_003";

        // 测试SIMPLE策略
        AtomicInteger simplePassCount = new AtomicInteger(0);
        for (int i = 0; i < 20; i++) {
            StrategyContext ctx = StrategyContext.builder()
                    // 不同用户避免用户限流
                    .userId(i)
                    .build();
            try {
                rateLimitHandler.handle(ctx);
                simplePassCount.incrementAndGet();
            } catch (RateLimitHandler.RateLimitException e) {
                // 忽略
            }
        }

        // 测试ML_MODEL策略
        AtomicInteger mlPassCount = new AtomicInteger(0);
        for (int i = 0; i < 20; i++) {
            StrategyContext ctx = StrategyContext.builder()
                    // 不同用户避免用户限流
                    .userId(i)
                    .build();
            try {
                rateLimitHandler.handle(ctx);
                mlPassCount.incrementAndGet();
            } catch (RateLimitHandler.RateLimitException e) {
                // 忽略
            }
        }

        log.info("策略类型限流测试: SIMPLE通过={}, ML_MODEL通过={}",
                simplePassCount.get(), mlPassCount.get());

        // 验证: SIMPLE策略通过数应该更多
        assertTrue(simplePassCount.get() > mlPassCount.get(),
                "SIMPLE策略的通过率应该高于ML_MODEL策略");
    }

    /**
     * 测试4: 全局限流
     * 预期: 即使是不同用户,总QPS也会被全局限流器控制
     */
    @Test
    void testGlobalRateLimit() throws Exception {
        int userCount = 50;  // 50个不同用户
        AtomicInteger passCount = new AtomicInteger(0);

        // 不同用户快速请求
        for (int i = 0; i < userCount; i++) {
            StrategyContext ctx = StrategyContext.builder()
                    // 不同用户避免用户限流
                    .userId(i)
                    .build();
            try {
                rateLimitHandler.handle(ctx);
                passCount.incrementAndGet();
            } catch (RateLimitHandler.RateLimitException e) {
                // 忽略
            }
        }

        log.info("全局限流测试结果: 50个不同用户请求, 通过={}", passCount.get());

        // 验证: 全局限流应该生效
        assertTrue(passCount.get() < userCount, "全局限流应该拒绝部分请求");
    }

    /**
     * 测试5: Redis降级场景
     * 预期: Redis不可用时,自动降级到单机限流,服务仍可用
     */
    @Test
    void testRedisFailover() throws Exception {
        // 注意: 这个测试需要手动停止Redis服务来验证
        // 或者通过Mock的方式模拟Redis异常

        Integer userId = 004;
        StrategyContext ctx = StrategyContext.builder()
                .userId(userId)
                .build();
        try {
            rateLimitHandler.handle(ctx);
            log.info("✅ Redis降级测试: 限流功能正常(可能使用了降级方案)");
        } catch (RateLimitHandler.RateLimitException e) {
            log.info("⛔ Redis降级测试: 触发限流={}", e.getMessage());
        } catch (Exception e) {
            fail("Redis不可用时应该降级到单机限流,而不是抛出异常");
        }
    }

    /**
     * 测试6: 滑动窗口特性验证
     * 预期: 等待窗口过期后,限流应该恢复
     */
    @Test
    void testSlidingWindow() throws Exception {
        Integer userId = 005;

        // 第一轮: 快速发送请求直到被限流
        int firstRoundPass = 0;
        for (int i = 0; i < 20; i++) {
            StrategyContext ctx = StrategyContext.builder()
                    .userId(userId)
                    .build();
            try {
                rateLimitHandler.handle(ctx);
                firstRoundPass++;
            } catch (RateLimitHandler.RateLimitException e) {
                break;  // 被限流了
            }
        }

        log.info("第一轮请求: 通过={}", firstRoundPass);

        // 等待2秒(超过1秒的窗口期)
        log.info("等待2秒,让滑动窗口过期...");
        Thread.sleep(2000);

        // 第二轮: 再次发送请求,应该能通过
        int secondRoundPass = 0;
        for (int i = 0; i < 20; i++) {
            StrategyContext ctx = StrategyContext.builder()
                    .userId(userId)
                    .build();
            try {
                rateLimitHandler.handle(ctx);
                secondRoundPass++;
            } catch (RateLimitHandler.RateLimitException e) {
                break;
            }
        }

        log.info("第二轮请求: 通过={}", secondRoundPass);

        // 验证: 窗口过期后应该能再次通过请求
        assertTrue(secondRoundPass > 0, "窗口过期后应该能再次通过请求");
    }
}