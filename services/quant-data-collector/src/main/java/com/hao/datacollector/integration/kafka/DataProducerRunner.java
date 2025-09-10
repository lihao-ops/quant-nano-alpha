package com.hao.datacollector.integration.kafka;

import com.hao.datacollector.service.KafkaProducerService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.CommandLineRunner;
import org.springframework.stereotype.Component;

@Component
public class DataProducerRunner implements CommandLineRunner {

    @Autowired
    private KafkaProducerService producerService;

    @Override
    public void run(String... args) throws Exception {
        String topic = "quotation";
        // 模拟批量发送行情数据
        for (int i = 1; i <= 10; i++) {
            String windCode = "wind" + i;
            String json = "{ \"wind_code\":\"" + windCode + "\", \"latest_price\":" + (100 + i) + " }";
            producerService.send(topic, windCode, json);
        }
        System.out.println("已发送 10 条测试数据到 topic: " + topic);
    }
}
