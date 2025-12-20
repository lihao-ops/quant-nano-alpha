package com.hao.datacollector.study.jmm;

import lombok.extern.slf4j.Slf4j;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;

import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * JMMTest
 * 测试 Java 内存模型(JMM)的可见性、原子性和指令重排序特性
 */
@SpringBootTest
@Slf4j
public class JMMTest {

    // 可见性相关变量
    private volatile boolean volatileFlag = false;
    private boolean plainFlag = false;
    private final Object lock = new Object();
    private boolean syncFlag = false;

    // 原子性相关变量
    private int nonAtomicCounter = 0;
    private AtomicInteger atomicCounter = new AtomicInteger(0);

    // 指令重排序相关变量
    private int x = 0;
    private int y = 0;
    private volatile int v = 0;

    @Test
    void testVisibility() throws InterruptedException {
        log.info("===_测试可见性_===|Log_message");

        testVolatileVisibility();
        testPlainVisibility();
        testSynchronizedVisibility();
    }

    /**
     * 测试 volatile 变量的可见性
     * 思路：
     * 1. 写线程将 volatileFlag 设置为 true。
     * 2. 读线程轮询 volatileFlag，直到观察到 true。
     * 3. 因为 volatile 保证可见性，一旦写线程写入，读线程必然能看到最新值。
     * 结论：
     * 通过观察读线程能及时读到 true，证明了 volatile 提供了跨线程的可见性。
     */
    void testVolatileVisibility() throws InterruptedException {
        log.info("---_测试_volatile_变量的可见性_---");

        CountDownLatch latch = new CountDownLatch(2);

        // 写线程
        Thread writer = new Thread(() -> {
            volatileFlag = true;
            log.info("[写]_设置_volatileFlag_=_true");
            latch.countDown();
        }, "Writer-Volatile");

        // 读线程
        Thread reader = new Thread(() -> {
            // 轮询 volatileFlag 直到观察到 true
            while (!volatileFlag) {
                // busy-wait
            }
            log.info("[读]_观察到_volatileFlag_=_true");
            latch.countDown();
        }, "Reader-Volatile");

        writer.start();
        reader.start();
        latch.await(1, TimeUnit.SECONDS);

        assertTrue(volatileFlag, "volatileFlag 应被观察到为 true");
        log.info("[结论]_volatile_变量的写入对其他线程可见");
    }

    /**
     * 测试普通变量的可见性
     * 思路：
     * 1. 写线程将 plainFlag 设置为 true。
     * 2. 读线程在稍后读取 plainFlag。
     * 3. 因为普通变量没有可见性保证，写线程的更新不一定立即对其他线程可见。
     * 结论：
     * 通过读线程读取的值可能为 true 或 false，说明普通变量存在可见性问题。
     */
    void testPlainVisibility() throws InterruptedException {
        log.info("---_测试普通变量的可见性_---|Log_message");

        CountDownLatch latch = new CountDownLatch(2);

        // 写线程
        Thread writer = new Thread(() -> {
            plainFlag = true;
            log.info("[写]_设置_plainFlag_=_true");
            latch.countDown();
        }, "Writer-Plain");

        // 读线程
        Thread reader = new Thread(() -> {
            // 等待一段时间后读取 plainFlag
            try {
                Thread.sleep(100);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }
            log.info("[读]_读取_plainFlag_=_{}", plainFlag);
            latch.countDown();
        }, "Reader-Plain");

        writer.start();
        reader.start();
        latch.await(1, TimeUnit.SECONDS);

        // plainFlag 的可见性是不确定的,可能为 true 也可能为 false
        log.info("[结论]_普通变量的写入对其他线程的可见性是不确定的,存在可见性问题|Log_message");
    }

    /**
     * 测试 synchronized 块的可见性
     * 思路：
     * 1. 写线程在 synchronized 块内写 syncFlag。
     * 2. 读线程在同一个 lock 对象的 synchronized 块内读取 syncFlag。
     * 3. synchronized 保证了互斥性和内存可见性：
     *    - 进入 synchronized 块前，线程必须刷新工作内存；
     *    - 离开 synchronized 块后，会把修改同步回主内存。
     * 结论：
     * 读线程一定能看到写线程的最新值，证明 synchronized 块可以保证跨线程可见性。
     */
    void testSynchronizedVisibility() throws InterruptedException {
        log.info("---_测试_synchronized_块的可见性_---");

        CountDownLatch latch = new CountDownLatch(2);

        // 写线程
        Thread writer = new Thread(() -> {
            synchronized (lock) {
                syncFlag = true;
                log.info("[写]_在_synchronized_块内设置_syncFlag_=_true");
            }
            latch.countDown();
        }, "Writer-Sync");

        // 读线程
        Thread reader = new Thread(() -> {
            synchronized (lock) {
                log.info("[读]_在_synchronized_块内读取_syncFlag_=_{}", syncFlag);
            }
            latch.countDown();
        }, "Reader-Sync");

        writer.start();
        reader.start();
        latch.await(1, TimeUnit.SECONDS);

        assertTrue(syncFlag, "syncFlag 应被观察到为 true");
        log.info("[结论]_synchronized_块内的写入对其他线程是可见的");
    }

    @Test
    void testAtomicity() throws InterruptedException {
        log.info("===_测试原子性_===|Log_message");

        testNonAtomicIncrement();
        testAtomicIncrement();
    }

    void testNonAtomicIncrement() throws InterruptedException {
        log.info("---_测试非原子性_++_操作_---|Log_message");

        int threadCount = 32;
        int incrementsPerThread = 1000;
        ExecutorService pool = Executors.newFixedThreadPool(threadCount);
        CountDownLatch latch = new CountDownLatch(threadCount);

        nonAtomicCounter = 0;

        for (int i = 0; i < threadCount; i++) {
            pool.submit(() -> {
                for (int j = 0; j < incrementsPerThread; j++) {
                    nonAtomicCounter++;
                }
                latch.countDown();
            });
        }

        latch.await();
        pool.shutdown();

        int expected = threadCount * incrementsPerThread;
        log.info("[非原子性]_期望计数_=_{}，实际_nonAtomicCounter_=_{}", expected, nonAtomicCounter);

        assertTrue(nonAtomicCounter != expected, "nonAtomicCounter 应与期望值不同,证明 ++ 操作不是原子的");
        log.info("[结论]_非原子_++_操作存在数据丢失,导致最终结果与期望不符|Log_message");
    }

    void testAtomicIncrement() throws InterruptedException {
        log.info("---_测试_AtomicInteger_的原子性_---");

        int threadCount = 32;
        int incrementsPerThread = 1000;
        ExecutorService pool = Executors.newFixedThreadPool(threadCount);
        CountDownLatch latch = new CountDownLatch(threadCount);

        atomicCounter.set(0);

        for (int i = 0; i < threadCount; i++) {
            pool.submit(() -> {
                for (int j = 0; j < incrementsPerThread; j++) {
                    atomicCounter.incrementAndGet();
                }
                latch.countDown();
            });
        }

        latch.await();
        pool.shutdown();

        int expected = threadCount * incrementsPerThread;
        int actual = atomicCounter.get();
        log.info("[原子性]_期望计数_=_{}，实际_atomicCounter_=_{}", expected, actual);

        assertEquals(expected, actual, "atomicCounter 应等于期望值");
        log.info("[结论]_AtomicInteger_提供了原子性,多线程并发下不会出现数据丢失");
    }

    @Test
    void testReordering() throws InterruptedException {
        log.info("===_测试指令重排序_===|Log_message");

        final int loops = 100000;
        AtomicInteger reorderCount = new AtomicInteger(0);

        ExecutorService pool = Executors.newFixedThreadPool(Runtime.getRuntime().availableProcessors());
        CountDownLatch latch = new CountDownLatch(loops);

        for (int i = 0; i < loops; i++) {
            pool.submit(() -> {
                int r1 = -1;
                int r2 = -1;

                x = 0;
                y = 0;
                v = 0;

                // 模拟线程1
                x = 1;
                v = 1; // volatile 写作为内存屏障
                y = 1;

                // 模拟线程2
                while (v == 0) {
                    // busy-wait
                }
                r1 = y;
                r2 = x;

                if (r1 == 0 && r2 == 0) {
                    reorderCount.incrementAndGet();
                }

                latch.countDown();
            });
        }

        latch.await();
        pool.shutdownNow();

        int found = reorderCount.get();
        log.info("[重排序统计]_在_{}_次尝试中，共观察到_r1==0_&&_r2==0_的次数_=_{}", loops, found);

        assertTrue(found > 0, "应观察到由重排序导致的异常结果");
        log.info("[结论]_观察到可能由重排序或可见性导致的异常结果。使用_volatile/synchronized_可作为内存屏障避免此类现象。");
    }
}
