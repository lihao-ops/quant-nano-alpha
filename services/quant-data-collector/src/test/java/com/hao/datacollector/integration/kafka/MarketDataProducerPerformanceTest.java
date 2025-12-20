package com.hao.datacollector.integration.kafka;

import lombok.extern.slf4j.Slf4j;
import org.apache.kafka.clients.producer.RecordMetadata;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.kafka.core.KafkaTemplate;

import java.util.Random;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicLong;

@Slf4j
@SpringBootTest
public class MarketDataProducerPerformanceTest {

    @Autowired
    private KafkaTemplate<String, String> kafkaTemplate;

    /** ---------------------- 可调参数区域 ---------------------- **/
    private static final String TOPIC = "market-tick";     // 测试主题
    private static final int TARGET_QPS = 5400;            // 目标QPS
    private static final int TEST_SECONDS = 30;            // 压测时长 (秒)
    private static final int THREAD_COUNT = 4;             // 发送线程数，可调优
    private static final boolean VERIFY_CALLBACK = true;   // 是否开启Kafka发送确认验证
    /** -------------------------------------------------------- **/

    private final Random random = new Random();

    @Test
    void testMarketDataPerformance() throws Exception {

        int totalMsgCount = TARGET_QPS * TEST_SECONDS;
        CountDownLatch latch = new CountDownLatch(totalMsgCount);
        ExecutorService executor = Executors.newFixedThreadPool(THREAD_COUNT);

        AtomicLong successCount = new AtomicLong(0);
        AtomicLong errorCount = new AtomicLong(0);
        AtomicLong totalLatency = new AtomicLong(0);

        log.info("_启动行情数据性能压测|Log_message");
        log.info("Topic:_{},_QPS:_{},_时长:_{}s,_并发:_{},_校验回调:_{}",
                TOPIC, TARGET_QPS, TEST_SECONDS, THREAD_COUNT, VERIFY_CALLBACK);

        long startTime = System.currentTimeMillis();

        for (int sec = 0; sec < TEST_SECONDS; sec++) {
            long secondStart = System.currentTimeMillis();

            for (int i = 0; i < TARGET_QPS; i++) {
                executor.submit(() -> {
                    long sendStart = System.nanoTime();
                    String msg = buildMockMarketJson();

                    kafkaTemplate.send(TOPIC, msg).whenComplete((result, ex) -> {
                        latch.countDown();
                        long cost = (System.nanoTime() - sendStart) / 1_000_000; // ms
                        totalLatency.addAndGet(cost);

                        if (ex != null) {
                            errorCount.incrementAndGet();
                            log.error("日志记录|Log_message,_Send_failed:_{}", ex.getMessage(), ex);
                        } else if (VERIFY_CALLBACK) {
                            RecordMetadata meta = result.getRecordMetadata();
                            if (meta != null) {
                                successCount.incrementAndGet();
                            } else {
                                errorCount.incrementAndGet();
                                log.warn("日志记录|Log_message,_Send_success_but_no_metadata");
                            }
                        } else {
                            successCount.incrementAndGet();
                        }
                    });
                });
            }

            long cost = System.currentTimeMillis() - secondStart;
            double achievedQps = TARGET_QPS * 1000.0 / Math.max(cost, 1);
            log.info("_第{}秒发送完毕,_耗时_{}_ms,_实际QPS_≈_{}", sec + 1, cost, (int) achievedQps);

            // 保证每秒控制在 1s 内节奏
            TimeUnit.MILLISECONDS.sleep(Math.max(0, 1000 - cost));
        }

        latch.await(); // 等待所有消息发送完成
        long totalCost = System.currentTimeMillis() - startTime;

        double avgLatency = totalLatency.get() * 1.0 / Math.max(successCount.get(), 1);
        double avgQps = successCount.get() * 1000.0 / totalCost;

        log.info("_测试结果汇总_===============================|Log_message");
        log.info("总发送消息数:_{}|Log_message", totalMsgCount);
        log.info("成功消息数:_{}|Log_message", successCount);
        log.info("失败消息数:_{}|Log_message", errorCount);
        log.info("平均延迟:_{}_ms", String.format("%.2f", avgLatency));
        log.info("平均QPS:_{}", String.format("%.2f", avgQps));
        log.info("总耗时:_{}_秒|Log_message", totalCost / 1000.0);
        log.info("日志记录|Log_message");

        executor.shutdown();
    }

    /** 构造模拟行情数据 **/
    private String buildMockMarketJson() {
        double price = 100 + random.nextDouble() * 5;
        double volume = random.nextInt(1000) + random.nextDouble();
        return String.format(
                "{\"symbol\":\"SZ000001\",\"price\":%.2f,\"volume\":%.2f,\"timestamp\":%d}",
                price, volume, System.currentTimeMillis());
    }
}
