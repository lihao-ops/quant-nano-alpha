package com.quant.audit.integration.kafka;

import integration.kafka.KafkaConstants;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.kafka.support.Acknowledgment;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;
import org.springframework.stereotype.Service;

import java.util.concurrent.atomic.AtomicInteger;

@Slf4j
@Service
public class KafkaConsumerService {

    private final AtomicInteger counter = new AtomicInteger(0);
    private volatile long windowStart = System.currentTimeMillis();
    private volatile String firstMessage = null;
    private volatile String lastMessage = null;

    private final ThreadPoolTaskExecutor ioTaskExecutor;

    public KafkaConsumerService(@Qualifier("ioTaskExecutor") ThreadPoolTaskExecutor ioTaskExecutor) {
        this.ioTaskExecutor = ioTaskExecutor;
    }

    @KafkaListener(
            topics = KafkaConstants.TOPIC_QUOTATION,
            groupId = KafkaConstants.GROUP_AUDIT_SERVICE,
            containerFactory = KafkaConstants.LISTENER_CONTAINER_FACTORY
    )
    public void consume(String message, Acknowledgment ack) {  // ⚠️ 注意这里加了 Acknowledgment
        try {
            long now = System.currentTimeMillis();
            // 记录第一条消息
            if (firstMessage == null) {
                firstMessage = message;
            }
            // 每条消息都更新 lastMessage
            lastMessage = message;
            // 增加计数
            int count = counter.incrementAndGet();
            // 每秒输出一次统计
            if (now - windowStart >= 1000) {
                log.info("Thread_name={}, 消息处理量={}条/s, 本秒第一条消息={}, 最后一条消息={}",
                        Thread.currentThread().getName(),
                        count,
                        firstMessage,
                        lastMessage);
                // 重置计数器和窗口
                counter.set(0);
                firstMessage = null;
                lastMessage = null;
                windowStart = now;
            }
            // 手动提交 offset
            ack.acknowledge();
        } catch (Exception e) {
            log.error("消息处理异常", e);
            // 不提交 offset，消息会重试
        }
//         TODO: 这里可以根据 wind_code 解析消息 JSON，然后处理策略
    }
}
