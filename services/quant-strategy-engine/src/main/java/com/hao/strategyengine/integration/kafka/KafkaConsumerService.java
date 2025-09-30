package com.hao.strategyengine.integration.kafka;

import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.kafka.support.Acknowledgment;
import org.springframework.stereotype.Service;

@Slf4j
@Service
public class KafkaConsumerService {

    @KafkaListener(
            topics = "quotation",
            groupId = "strategy-service-group",
            containerFactory = "kafkaListenerContainerFactory"
    )
    public void consume(String message, Acknowledgment ack) {  // ⚠️ 注意这里加了 Acknowledgment
//        try {
//            log.info("收到消息={}", message);
//            // 处理消息
//            ack.acknowledge(); // 手动提交 offset
//        } catch (Exception e) {
//            e.printStackTrace();
//            // 不提交 offset，下次会重试消费
//        }
        // TODO: 这里可以根据 wind_code 解析消息 JSON，然后处理策略
        // 示例：
        // ObjectMapper mapper = new ObjectMapper();
        // HistoryTrendDTO dto = mapper.readValue(message, HistoryTrendDTO.class);
    }
}
