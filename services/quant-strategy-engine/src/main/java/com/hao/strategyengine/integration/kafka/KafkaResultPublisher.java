package com.hao.strategyengine.integration.kafka;

import com.alibaba.fastjson.JSON;
import com.hao.strategyengine.model.response.StrategyResultBundle;
import lombok.RequiredArgsConstructor;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Component;

/**
 * @author hli
 * @program: quant-nano-alpha
 * @Date 2025-10-22 20:12:26
 * @description:
 */

@Component
@RequiredArgsConstructor
public class KafkaResultPublisher {
    private final KafkaTemplate kafka;

    public void publish(String topic, StrategyResultBundle bundle) {
        kafka.send(topic, bundle.getComboKey(), JSON.toJSONString(bundle));
    }
}