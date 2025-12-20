package com.hao.strategyengine.integration.kafka;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.extern.slf4j.Slf4j;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.kafka.annotation.KafkaListener;

import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicLong;

/**
 *  说明：
 * 模拟策略微服务侧的Kafka消费者性能测试。
 * 功能包括：
 * - 实时统计消费速率QPS
 * - 统计延迟（发送→消费）
 * - 丢消息检测（通过timestamp或sequence）
 * - 输出详细日志，验证端到端性能表现
 */
@Slf4j
@SpringBootTest
public class MarketDataConsumerPerformanceTest {

    /**
     * ---------------------- 可调参数 ----------------------
     **/
    private static final String TOPIC = "market-tick";     // 消费主题
    private static final int TEST_SECONDS = 30;            // 测试持续时间
    private static final boolean VERIFY_LATENCY = true;    // 是否计算生产-消费延迟
    private static final boolean ENABLE_LOSS_CHECK = true; // 是否检测丢消息
    /**
     * -----------------------------------------------------
     **/

    private static final ObjectMapper MAPPER = new ObjectMapper();

    private final AtomicLong consumeCount = new AtomicLong(0);
    private final AtomicLong totalLatency = new AtomicLong(0);
    private final AtomicLong lastTimestamp = new AtomicLong(-1);
    private final AtomicLong lossCount = new AtomicLong(0);

    private final ScheduledExecutorService metricsScheduler = Executors.newScheduledThreadPool(1);

    /**
     * 启动消费者测试逻辑
     */
    @Test
    void testMarketDataConsumerPerformance() throws Exception {
        log.info("_启动行情消费者性能测试|Log_message");
        log.info("Topic:_{},_持续时间:_{}s,_计算延迟:_{},_检查丢失:_{}",
                TOPIC, TEST_SECONDS, VERIFY_LATENCY, ENABLE_LOSS_CHECK);

        // 定时输出性能指标
        metricsScheduler.scheduleAtFixedRate(() -> {
            long count = consumeCount.get();
            double avgLatency = VERIFY_LATENCY && count > 0
                    ? totalLatency.get() * 1.0 / count : 0;
            log.info("_当前统计_->_已消费:_{},_平均延迟:_{}_ms,_估算QPS:_{}",
                    count, String.format("%.2f", avgLatency), count / (double) TEST_SECONDS);
        }, 1, 2, TimeUnit.SECONDS);

        // 等待持续时间
        TimeUnit.SECONDS.sleep(TEST_SECONDS);

        metricsScheduler.shutdownNow();
        log.info("_消费性能测试完成_=============================|Log_message");
        log.info("总消费消息数:_{}|Log_message", consumeCount.get());
        log.info("总丢失消息数:_{}|Log_message", lossCount.get());
        log.info("平均延迟:_{}_ms", String.format("%.2f",
                totalLatency.get() * 1.0 / Math.max(1, consumeCount.get())));
        log.info("日志记录|Log_message");
    }

    /**
     * Kafka消息监听器
     */
    @KafkaListener(topics = TOPIC, groupId = "strategy-consumer-test")
    public void consumeMarketData(ConsumerRecord<String, String> record) {
        try {
            JsonNode json = MAPPER.readTree(record.value());
            long now = System.currentTimeMillis();
            long ts = json.path("timestamp").asLong();

            if (VERIFY_LATENCY && ts > 0) {
                totalLatency.addAndGet(now - ts);
            }

            if (ENABLE_LOSS_CHECK) {
                long prev = lastTimestamp.getAndSet(ts);
                if (prev > 0 && ts - prev > 200) { // 超过200ms认为可能丢消息
                    lossCount.incrementAndGet();
                    log.warn("_可能丢失消息,_上条时间戳={},_当前={}|Log_message", prev, ts);
                }
            }

            consumeCount.incrementAndGet();

        } catch (Exception e) {
            log.error("_消息消费异常:_{}|Log_message", e.getMessage(), e);
        }
    }
}
