package com.hao.strategyengine.strategy.lock;

import com.hao.strategyengine.common.model.response.StrategyResultBundle;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.redisson.api.RLock;
import org.redisson.api.RedissonClient;
import org.springframework.boot.test.context.SpringBootTest;

import java.lang.reflect.Field;
import java.util.Collections;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.fail;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * ===============================================================
 * 【类名】：DistributedLockServiceInterviewTest
 * ===============================================================
 * <p>
 * 【面试官视角的背景说明】：
 * 1. 量化策略引擎在生产环境是多实例部署，每个实例都可能被同一批行情驱动并触发同一组策略计算。
 * 2. 如果不做幂等控制，会导致「重复计算 + Redis/Kafka 双写」等级联问题，资源白白浪费。
 * 3. 现有实现选择 Redisson 分布式锁 + 本地 pending Future 的混合方案，是从真实面试中常考的
 *    “缓存击穿 + 并发请求合并” 场景推演出来的。
 * <p>
 * 【测试目标】：
 * ⦿ 用可执行的单测验证该实现的收益（只计算一次、失败分支等待已有结果）。
 * ⦿ 通过注释说明当前方案的 trade-off，并指出潜在的优化方向（如跨实例 result sharing）。
 * <p>
 * 【注释风格要求】：
 * 以大厂面试官视角，把为什么要这么设计、能解决什么痛点、后续怎么演进，都写在类/方法/核心代码注释中。
 */
@ExtendWith(MockitoExtension.class)
@SpringBootTest
public class DistributedLockServiceInterviewTest {

    @Mock
    private RedissonClient redissonClient;

    @Mock
    private RLock rLock;

    private DistributedLockService lockService;

    @BeforeEach
    void setUp() {
        when(redissonClient.getLock(anyString())).thenReturn(rLock);
        lockService = new DistributedLockService(redissonClient);
    }

    /**
     * ===============================================================
     * 【面试问题点 1】：锁拿到时是否真的做到「只算一次」？
     * ===============================================================
     * <p>
     * 作为面试官，我会追问：如果两个线程几乎同时打进来，第一个抢到锁后释放，第二个会不会重复计算？
     * 下面这个用例给出事实依据：
     * 1. 模拟 Redisson.tryLock 成功，确认 compute Supplier 只会被执行一次；
     * 2. 验证 unlock 被调用，说明资源能够正确释放，避免死锁；
     * 3. 断言返回值就是业务方需要的 StrategyResultBundle。
     */
    @Test
    void shouldComputeOnceWhenLockIsAcquired() throws InterruptedException {
        // Step 1️⃣ 模拟分布式锁成功获取
        when(rLock.tryLock(eq(0L), eq(1L), eq(TimeUnit.SECONDS))).thenReturn(true);

        AtomicInteger computeTimes = new AtomicInteger();
        StrategyResultBundle expected = new StrategyResultBundle("combo-A", Collections.emptyList());

        StrategyResultBundle actual = lockService.acquireOrWait("combo-A", () -> {
            // Step 2️⃣ Supplier 是真正的重活，计数器帮助我们证明只执行一次
            computeTimes.incrementAndGet();
            return expected;
        });

        // Step 3️⃣ 验证 Supplier 被调用一次，锁释放一次，返回值正确
        assertThat(computeTimes.get()).isEqualTo(1);
        verify(rLock).unlock();
        assertThat(actual).isSameAs(expected);
    }

    /**
     * ===============================================================
     * 【面试问题点 2】：抢锁失败的线程如何复用结果？是否存在优化空间？
     * ===============================================================
     * <p>
     * 我会继续追问：当锁已经在别的实例手里时，本实例等待逻辑是否靠谱？会不会因为 pending 映射只保存在本地
     * 而跨实例不可见，从而导致仍然重复计算？
     * 这个测试：
     * 1. 强制 tryLock 返回 false，让代码走「等待 pending future」分支；
     * 2. 通过反射拿到 pending map，模拟远端线程完成计算后填充结果；
     * 3. 验证本线程确实拿到已有结果，且不会二次执行 Supplier。
     * <p>
     * ⚠️ 反射只是为了测试可控性，同时也凸显一个优化点：生产环境应考虑把 pending 状态共享到 Redis/消息总线，
     * 让其他节点也能第一时间复用结果，减少超时风险。
     */
    @Test
    void shouldWaitForExistingResultWhenLockIsHeldByOtherInstance() throws Exception {
        // Step 1️⃣ 锁竞争失败，模拟其它实例已经持有锁
        when(rLock.tryLock(eq(0L), eq(1L), eq(TimeUnit.SECONDS))).thenReturn(false);

        AtomicReference<StrategyResultBundle> resultRef = new AtomicReference<>();
        StrategyResultBundle expected = new StrategyResultBundle("combo-B", Collections.emptyList());

        Thread waitingThread = new Thread(() -> {
            StrategyResultBundle actual = lockService.acquireOrWait("combo-B", () -> {
                // Step 2️⃣ 如果 Supplier 被执行，说明幂等失效 -> 直接失败，暴露问题
                fail("锁已被占用时不应重复执行计算逻辑");
                return expected;
            });
            resultRef.set(actual);
        });
        waitingThread.start();

        // Step 3️⃣ 通过反射拿到 pending 映射，模拟远端节点完成计算
        Field pendingField = DistributedLockService.class.getDeclaredField("pending");
        pendingField.setAccessible(true);
        @SuppressWarnings("unchecked")
        Map<String, CompletableFuture<StrategyResultBundle>> pendingMap =
                (Map<String, CompletableFuture<StrategyResultBundle>>) pendingField.get(lockService);

        CompletableFuture<StrategyResultBundle> future;
        // Step 4️⃣ 自旋等待 pending future 被创建，体现「等待已有任务」的核心设计
        int spin = 0;
        while ((future = pendingMap.get("combo-B")) == null && spin++ < 50) {
            Thread.sleep(10);
        }
        if (future == null) {
            fail("未能在预期时间内创建 pending future，说明等待逻辑存在缺陷");
        }

        // Step 5️⃣ 模拟远端线程把结果广播给等待者
        future.complete(expected);

        waitingThread.join();

        // Step 6️⃣ 验证未触发本地重复计算，且成功拿到现有结果
        assertThat(resultRef.get()).isSameAs(expected);
        verify(rLock, never()).unlock();
    }
}
