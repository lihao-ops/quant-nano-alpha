package com.quant.audit.integration.kafka;

import integration.kafka.KafkaConstants;
import lombok.extern.slf4j.Slf4j;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.kafka.support.Acknowledgment;
import org.springframework.stereotype.Service;

@Slf4j
@Service
public class LogConsumerService {

    @KafkaListener(
            topics = {
                    KafkaConstants.TOPIC_LOG_SERVICE_ORDER,
                    KafkaConstants.TOPIC_LOG_QUANT_XXL_JOB,
                    KafkaConstants.TOPIC_LOG_QUANT_DATA_COLLECTOR,
                    KafkaConstants.TOPIC_LOG_QUANT_STRATEGY_ENGINE,
                    KafkaConstants.TOPIC_LOG_QUANT_RISK_CONTROL,
                    KafkaConstants.TOPIC_LOG_QUANT_AUDIT_SERVICE
            },
            groupId = KafkaConstants.GROUP_AUDIT_SERVICE,
            containerFactory = KafkaConstants.LISTENER_CONTAINER_FACTORY
    )
    public void consumeLog(ConsumerRecord<String, String> record, Acknowledgment ack) {
        try {
            String topic = record.topic();
            String key = record.key();
            String value = record.value();
            // 记录接收的日志信息，包含服务与主机维度（key=app|ip）
            log.info("[AUDIT-LOG] topic={} key={} value={}", topic, key, value);
            // 可在此处解析结构化日志并存储到数据库/ES
            ack.acknowledge();
        } catch (Exception e) {
            log.error("日志消费异常", e);
            // 不提交 offset，交由重试处理
        }
    }
}