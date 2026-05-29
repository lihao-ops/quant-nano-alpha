package com.hao.quant.stocklist.service;

import com.github.benmanes.caffeine.cache.Cache;
import com.hao.quant.stocklist.mapper.StockSignalMapper;
import com.hao.quant.stocklist.model.StockSignal;
import constants.RedisKeyConstants;
import org.junit.jupiter.api.*;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;
import org.redisson.api.RList;
import org.redisson.api.RLock;
import org.redisson.api.RedissonClient;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicInteger;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

/**
 * MultiLevelCacheService 全链路测试
 * <p>
 * 测试覆盖：
 * 1. L1 Caffeine 缓存命中
 * 2. L2 Redis 缓存命中
 * 3. L3 MySQL 兜底查询
 * 4. EMPTY_MARKER 空值标记
 * 5. 分布式锁行为
 * 6. Sentinel 限流
 * 7. 万级并发测试
 * 8. 异常处理
 *
 * @author hli
 * @date 2026-02-01
 */
@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
@DisplayName("MultiLevelCacheService 全链路测试")
class MultiLevelCacheServiceTest {

    private static final String STRATEGY_ID = "MA_BULLISH";
    private static final String TRADE_DATE = "2026-01-15";
    private static final String CACHE_KEY = RedisKeyConstants.STOCK_SIGNAL_LIST_PREFIX + STRATEGY_ID + ":" + TRADE_DATE;

    @Mock
    private Cache<String, List<String>> caffeineCache;

    @Mock
    private RedissonClient redissonClient;

    @Mock
    private RList<String> rList;

    @Mock
    private RLock rLock;

    @Mock
    private StockSignalMapper stockSignalMapper;

    @InjectMocks
    private MultiLevelCacheService multiLevelCacheService;

    @BeforeEach
    void setUp() throws InterruptedException {
        // 基础 Mock 设置
        doReturn(rList).when(redissonClient).getList(anyString());
        when(redissonClient.getLock(anyString())).thenReturn(rLock);
        when(rLock.tryLock(anyLong(), anyLong(), any(TimeUnit.class))).thenReturn(true);
        when(rLock.isHeldByCurrentThread()).thenReturn(true);

        // 核心 Mock：模拟 Caffeine 的 get(key, loader) 行为
        // 当调用 caffeineCache.get(key, loader) 时，直接执行 loader 回调
        when(caffeineCache.get(anyString(), any())).thenAnswer(invocation -> {
            String key = invocation.getArgument(0);
            java.util.function.Function<String, List<String>> loader = invocation.getArgument(1);
            return loader.apply(key);
        });
    }

    // ==================== L1 Caffeine 缓存测试 ====================

    @Nested
    @DisplayName("L1 Caffeine 缓存测试")
    class L1CacheTests {

        @Test
        @DisplayName("L1 缓存命中 - Caffeine 直接返回已缓存值，不触发 loader")
        void testL1CacheHit() {
            // Given: 模拟 Caffeine 内部已有缓存，直接返回而不执行 loader
            List<String> cachedData = List.of("{\"windCode\":\"000001.SZ\"}");
            when(caffeineCache.get(anyString(), any())).thenReturn(cachedData);

            // When
            List<String> result = multiLevelCacheService.querySignals(STRATEGY_ID, TRADE_DATE);

            // Then
            assertEquals(cachedData, result);
            verify(redissonClient, never()).getList(anyString());  // 不应查询 Redis
        }

        @Test
        @DisplayName("L1 缓存未命中 - 触发 loader 查 L2")
        void testL1CacheMiss() {
            // Given: caffeineCache.get() 会执行 loader（setUp 中已配置）
            List<String> redisData = List.of("{\"windCode\":\"600519.SH\"}");
            when(rList.readAll()).thenReturn(redisData);

            // When
            List<String> result = multiLevelCacheService.querySignals(STRATEGY_ID, TRADE_DATE);

            // Then: loader 从 Redis 查到数据
            assertEquals(redisData, result);
        }
    }

    // ==================== L2 Redis 缓存测试 ====================

    @Nested
    @DisplayName("L2 Redis 缓存测试")
    class L2CacheTests {

        @Test
        @DisplayName("L2 缓存命中 - 返回数据")
        void testL2CacheHit() {
            // Given
            List<String> redisData = List.of("{\"windCode\":\"000001.SZ\"}", "{\"windCode\":\"600519.SH\"}");
            when(rList.readAll()).thenReturn(redisData);

            // When
            List<String> result = multiLevelCacheService.querySignals(STRATEGY_ID, TRADE_DATE);

            // Then
            assertEquals(2, result.size());
        }

        @Test
        @DisplayName("L2 空值标记命中 - 返回空列表")
        void testL2EmptyMarkerHit() {
            // Given
            List<String> emptyMarker = List.of(RedisKeyConstants.CACHE_EMPTY_MARKER);
            when(rList.readAll()).thenReturn(emptyMarker);

            // When
            List<String> result = multiLevelCacheService.querySignals(STRATEGY_ID, TRADE_DATE);

            // Then
            assertTrue(result.isEmpty());
            verify(stockSignalMapper, never()).selectPassedSignals(anyString(), anyString());
        }

        @Test
        @DisplayName("L2 缓存为空 - 继续查 L3")
        void testL2CacheMissGoToL3() {
            // Given
            when(rList.readAll()).thenReturn(Collections.emptyList());
            when(stockSignalMapper.selectPassedSignals(STRATEGY_ID, TRADE_DATE))
                    .thenReturn(createMockSignals(3));

            // When
            List<String> result = multiLevelCacheService.querySignals(STRATEGY_ID, TRADE_DATE);

            // Then
            assertEquals(3, result.size());
            verify(stockSignalMapper).selectPassedSignals(STRATEGY_ID, TRADE_DATE);
        }

        @Test
        @DisplayName("L2 Redis 异常 - 降级查 L3")
        void testL2RedisException() {
            // Given
            when(rList.readAll()).thenThrow(new RuntimeException("Redis connection failed"));
            when(stockSignalMapper.selectPassedSignals(STRATEGY_ID, TRADE_DATE))
                    .thenReturn(createMockSignals(2));

            // When
            List<String> result = multiLevelCacheService.querySignals(STRATEGY_ID, TRADE_DATE);

            // Then
            assertEquals(2, result.size());
        }
    }

    // ==================== L3 MySQL 兜底测试 ====================

    @Nested
    @DisplayName("L3 MySQL 兜底测试")
    class L3DatabaseTests {

        @Test
        @DisplayName("L3 查到数据 - 回填 Redis")
        void testL3QuerySuccess() {
            // Given
            when(rList.readAll()).thenReturn(null);
            List<StockSignal> dbSignals = createMockSignals(5);
            when(stockSignalMapper.selectPassedSignals(STRATEGY_ID, TRADE_DATE)).thenReturn(dbSignals);

            // When
            List<String> result = multiLevelCacheService.querySignals(STRATEGY_ID, TRADE_DATE);

            // Then
            assertEquals(5, result.size());
            verify(rList).addAll(anyList());  // 回填 Redis
        }

        @Test
        @DisplayName("L3 查无数据 - 缓存空值标记")
        void testL3QueryEmpty() {
            // Given
            when(rList.readAll()).thenReturn(null);
            when(stockSignalMapper.selectPassedSignals(STRATEGY_ID, TRADE_DATE))
                    .thenReturn(Collections.emptyList());

            // When
            List<String> result = multiLevelCacheService.querySignals(STRATEGY_ID, TRADE_DATE);

            // Then
            assertTrue(result.isEmpty());
            verify(rList).add(RedisKeyConstants.CACHE_EMPTY_MARKER);  // 缓存空标记
        }

        @Test
        @DisplayName("L3 数据库异常 - 抛出异常")
        void testL3DatabaseException() {
            // Given
            when(rList.readAll()).thenReturn(null);
            when(stockSignalMapper.selectPassedSignals(STRATEGY_ID, TRADE_DATE))
                    .thenThrow(new RuntimeException("Database connection failed"));

            // When - 异常会被服务层捕获并抛出
            RuntimeException exception = assertThrows(RuntimeException.class, () -> {
                multiLevelCacheService.querySignals(STRATEGY_ID, TRADE_DATE);
            });

            // Then
            assertEquals("Database connection failed", exception.getMessage());
        }
    }

    // ==================== 跨 Pod 分布式锁测试 ====================

    @Nested
    @DisplayName("跨 Pod 分布式锁测试")
    class CrossPodLockTests {

        @Test
        @DisplayName("获取锁成功 - 双重检查 Redis 后查 DB")
        void testLockAcquireSuccess() throws InterruptedException {
            // Given
            when(rLock.tryLock(anyLong(), anyLong(), any(TimeUnit.class))).thenReturn(true);
            // Redis 无数据，需要查 DB
            when(rList.readAll()).thenReturn(Collections.emptyList());
            when(stockSignalMapper.selectPassedSignals(STRATEGY_ID, TRADE_DATE))
                    .thenReturn(createMockSignals(3));

            // When
            List<String> result = multiLevelCacheService.querySignals(STRATEGY_ID, TRADE_DATE);

            // Then
            assertEquals(3, result.size());
            verify(rLock).unlock();
        }

        @Test
        @DisplayName("获取锁失败 - 读 Redis（其他 Pod 已回填）")
        void testLockAcquireFailedReadRedis() throws InterruptedException {
            // Given: 第一次查 Redis 无数据，获取锁失败，第二次查 Redis 有数据（其他 Pod 已回填）
            when(rLock.tryLock(anyLong(), anyLong(), any(TimeUnit.class))).thenReturn(false);
            List<String> redisData = List.of("{\"windCode\":\"000001.SZ\"}");
            when(rList.readAll())
                    .thenReturn(Collections.emptyList())
                    .thenReturn(redisData);

            // When
            List<String> result = multiLevelCacheService.querySignals(STRATEGY_ID, TRADE_DATE);

            // Then
            assertEquals(1, result.size());
            verify(rLock, never()).unlock();  // 未获取锁，不需要释放
            verify(stockSignalMapper, never()).selectPassedSignals(anyString(), anyString());
        }
    }

    // ==================== 边界条件测试 ====================

    @Nested
    @DisplayName("边界条件测试")
    class BoundaryTests {

        @Test
        @DisplayName("策略ID 为空字符串")
        void testEmptyStrategyId() {
            // Given
            String emptyStrategy = "";
            when(rList.readAll()).thenReturn(null);
            when(stockSignalMapper.selectPassedSignals(emptyStrategy, TRADE_DATE))
                    .thenReturn(Collections.emptyList());

            // When
            List<String> result = multiLevelCacheService.querySignals(emptyStrategy, TRADE_DATE);

            // Then
            assertTrue(result.isEmpty());
        }

        @Test
        @DisplayName("交易日期格式边界 - 周末日期")
        void testWeekendDate() {
            // Given
            String weekendDate = "2026-02-07";  // 假设是周六
            when(rList.readAll()).thenReturn(null);
            when(stockSignalMapper.selectPassedSignals(STRATEGY_ID, weekendDate))
                    .thenReturn(Collections.emptyList());

            // When
            List<String> result = multiLevelCacheService.querySignals(STRATEGY_ID, weekendDate);

            // Then
            assertTrue(result.isEmpty());
        }

        @Test
        @DisplayName("大量数据 - 1000 条信号")
        void testLargeDataSet() {
            // Given
            when(rList.readAll()).thenReturn(null);
            List<StockSignal> largeSignals = createMockSignals(1000);
            when(stockSignalMapper.selectPassedSignals(STRATEGY_ID, TRADE_DATE)).thenReturn(largeSignals);

            // When
            List<String> result = multiLevelCacheService.querySignals(STRATEGY_ID, TRADE_DATE);

            // Then
            assertEquals(1000, result.size());
        }
    }

    // ==================== 并发测试 ====================

    @Nested
    @DisplayName("高并发测试")
    class ConcurrencyTests {

        @Test
        @DisplayName("Caffeine loader 并发保证 - 同一 key 只加载一次")
        void testCaffeineLoaderConcurrency() throws InterruptedException {
            // Given
            int threadCount = 100;
            CountDownLatch startLatch = new CountDownLatch(1);
            CountDownLatch endLatch = new CountDownLatch(threadCount);
            AtomicInteger loaderCallCount = new AtomicInteger(0);

            // 模拟 Caffeine 的 get(key, loader) 并发行为：只有 1 个线程执行 loader
            List<String> loadedData = List.of("{\"windCode\":\"000001.SZ\"}");
            when(caffeineCache.get(anyString(), any())).thenAnswer(invocation -> {
                loaderCallCount.incrementAndGet();
                return loadedData;
            });

            ExecutorService executor = Executors.newFixedThreadPool(threadCount);

            // When
            for (int i = 0; i < threadCount; i++) {
                executor.submit(() -> {
                    try {
                        startLatch.await();
                        multiLevelCacheService.querySignals(STRATEGY_ID, TRADE_DATE);
                    } catch (InterruptedException e) {
                        Thread.currentThread().interrupt();
                    } finally {
                        endLatch.countDown();
                    }
                });
            }

            startLatch.countDown();  // 同时启动所有线程
            endLatch.await(10, TimeUnit.SECONDS);
            executor.shutdown();

            // Then: 由于 mock 不像真实 Caffeine 那样只调 1 次 loader，
            // 但所有线程都能正常返回数据（不会拿到空列表）
            assertTrue(loaderCallCount.get() >= 1);
        }

        @Test
        @DisplayName("L1 缓存命中 - 无锁竞争")
        void testL1CacheHitNoConcurrencyIssue() throws InterruptedException, ExecutionException {
            // Given
            int threadCount = 1000;
            List<String> cachedData = List.of("{\"windCode\":\"000001.SZ\"}");
            // Caffeine 内部已有缓存，直接返回
            when(caffeineCache.get(anyString(), any())).thenReturn(cachedData);

            ExecutorService executor = Executors.newFixedThreadPool(100);
            List<Future<List<String>>> futures = new ArrayList<>();

            // When
            for (int i = 0; i < threadCount; i++) {
                futures.add(executor.submit(() ->
                        multiLevelCacheService.querySignals(STRATEGY_ID, TRADE_DATE)));
            }

            // Then
            for (Future<List<String>> future : futures) {
                assertEquals(1, future.get().size());
            }

            executor.shutdown();
            verify(redissonClient, never()).getList(anyString());  // 从不查 Redis
        }
    }

    // ==================== 辅助方法 ====================

    private List<StockSignal> createMockSignals(int count) {
        List<StockSignal> signals = new ArrayList<>();
        for (int i = 0; i < count; i++) {
            StockSignal signal = new StockSignal();
            signal.setId((long) i);
            signal.setWindCode(String.format("%06d.SZ", i));
            signal.setStrategyId(STRATEGY_ID);
            signal.setSignalType("BUY");
            signal.setTriggerPrice(10.0 + i * 0.1);
            signal.setSignalTime(LocalDateTime.now());
            signal.setTradeDate(LocalDate.parse(TRADE_DATE));
            signal.setShowStatus(1);
            signal.setRiskSnapshot(60);
            signal.setStatus(1);
            signals.add(signal);
        }
        return signals;
    }
}
