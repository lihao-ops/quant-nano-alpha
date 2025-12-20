package com.hao.datacollector.service;

import com.alibaba.fastjson.JSON;
import com.hao.datacollector.dto.quotation.HistoryTrendDTO;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;

@Slf4j
@Service
public class KafkaProducerService {

    private final KafkaTemplate<String, String> kafkaTemplate;

    private static final int BATCH_SIZE = 500; // 每批发送条数

    private final ThreadPoolTaskExecutor executor; // 注入 IO线程池

    //量化/IO线程池
    public KafkaProducerService(KafkaTemplate<String, String> kafkaTemplate,
                                @Qualifier("ioTaskExecutor") ThreadPoolTaskExecutor executor) {
        this.kafkaTemplate = kafkaTemplate;
        this.executor = executor;
    }

    /**
     * 单条发送（原有方法不变）
     */
    public void send(String topic, String key, HistoryTrendDTO value) {
        String json = JSON.toJSONString(value);
        kafkaTemplate.send(topic, key, json)
                .whenComplete((result, ex) -> {
                    if (ex != null) {
                        log.error("日志记录|Log_message,kafka—sendError!_key={},_error={}", key, ex.getMessage(), ex);
                    }
                });
    }

    /**
     * 高性能批量发送
     * - 按 BATCH_SIZE 拆分
     * - 每个批次异步提交线程池执行
     */
    public void sendBatchHighPerformance(String topic, List<HistoryTrendDTO> data) {
        if (data == null || data.isEmpty()) return;

        List<HistoryTrendDTO> batchList = new ArrayList<>(BATCH_SIZE);
        for (HistoryTrendDTO dto : data) {
            batchList.add(dto);
            if (batchList.size() >= BATCH_SIZE) {
                submitBatch(topic, new ArrayList<>(batchList));
                batchList.clear();
            }
        }
        // 发送剩余不足 BATCH_SIZE 的数据
        if (!batchList.isEmpty()) {
            submitBatch(topic, batchList);
        }
    }

    /**
     * 将单个批次提交给线程池异步发送
     */
    private void submitBatch(String topic, List<HistoryTrendDTO> batch) {
        executor.execute(() -> {
            for (HistoryTrendDTO dto : batch) {
                try {
                    send(topic, dto.getWindCode(), dto);
                } catch (Exception e) {
                    log.error("批量发送失败|Batch_send_failed,windCode={},error={}", dto.getWindCode(), e.getMessage(), e);
                }
            }
        });
    }
}
