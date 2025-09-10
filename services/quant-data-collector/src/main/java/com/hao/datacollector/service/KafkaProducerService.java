package com.hao.datacollector.service;

import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Service;

@Service
public class KafkaProducerService {

    private final KafkaTemplate<String, String> kafkaTemplate;

    public KafkaProducerService(KafkaTemplate<String, String> kafkaTemplate) {
        this.kafkaTemplate = kafkaTemplate;
    }

    /**
     * 发送消息
     * @param topic topic名称
     * @param key   wind_code
     * @param value 数据 JSON 字符串
     */
    public void send(String topic, String key, String value) {
        kafkaTemplate.send(topic, key, value);
    }
}