package com.hao.strategyengine.integration.kafka;

import org.apache.kafka.clients.consumer.ConsumerConfig;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.apache.kafka.clients.consumer.ConsumerRecords;
import org.apache.kafka.clients.consumer.KafkaConsumer;
import org.apache.kafka.common.serialization.StringDeserializer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.io.BufferedReader;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.time.Duration;
import java.util.*;

@Component
public class Consumer2 {
    private static final Logger logger = LoggerFactory.getLogger(Consumer2.class);
    private static final String TOPIC = "ping-topic3";
    private static final String BOOTSTRAP_SERVERS = "192.168.254.2:9092";
    private static final String GROUP_ID = "test-consumer-group2";
    private static final String CONSUMED_IDS_FILE = "E:\\项目暂存\\quant-nano-alpha\\services\\quant-data-collector\\src\\main\\resources\\consumed_ids_group2.log";

    private static final String SENT_IDS_FILE = "E:\\项目暂存\\quant-nano-alpha\\services\\quant-data-collector\\src\\main\\resources\\sent_ids_group2.log";

    private KafkaConsumer<String, String> consumer;

    public void init() {
        logger.info("========== 初始化Consumer2 ==========");
        logger.info("Topic: {}", TOPIC);
        logger.info("Bootstrap Servers: {}", BOOTSTRAP_SERVERS);
        logger.info("Group ID: {}", GROUP_ID);

        Properties props = new Properties();
        props.put(ConsumerConfig.BOOTSTRAP_SERVERS_CONFIG, BOOTSTRAP_SERVERS);
        props.put(ConsumerConfig.GROUP_ID_CONFIG, GROUP_ID);
        props.put(ConsumerConfig.KEY_DESERIALIZER_CLASS_CONFIG, StringDeserializer.class.getName());
        props.put(ConsumerConfig.VALUE_DESERIALIZER_CLASS_CONFIG, StringDeserializer.class.getName());
        props.put(ConsumerConfig.AUTO_OFFSET_RESET_CONFIG, "earliest");
        props.put(ConsumerConfig.ENABLE_AUTO_COMMIT_CONFIG, "true");
        props.put(ConsumerConfig.MAX_POLL_RECORDS_CONFIG, "5000");
        props.put(ConsumerConfig.SESSION_TIMEOUT_MS_CONFIG, "30000");
        props.put(ConsumerConfig.REQUEST_TIMEOUT_MS_CONFIG, "40000");
        props.put(ConsumerConfig.ISOLATION_LEVEL_CONFIG, "read_committed");

        consumer = new KafkaConsumer<>(props);
        consumer.subscribe(Collections.singletonList(TOPIC));

        logger.info("Consumer2初始化完成");
        logger.info("====================================");
    }

    public void consumeAndVerify(int timeoutSeconds) {
        logger.info("========== 开始消费消息 ==========");
        logger.info("空闲超时时间: {} 秒", timeoutSeconds);
        logger.info("=================================");

        Set<Long> consumedIds = new HashSet<>();
        List<Long> allConsumedIds = new ArrayList<>();
        long consumedCount = 0;
        long startTime = System.currentTimeMillis();
        long lastMessageTime = startTime;

        try (FileWriter writer = new FileWriter(CONSUMED_IDS_FILE, false)) {

            while (true) {
                ConsumerRecords<String, String> records = consumer.poll(Duration.ofMillis(1000));

                if (records.isEmpty()) {
                    long idleTime = (System.currentTimeMillis() - lastMessageTime) / 1000;
                    if (idleTime > timeoutSeconds) {
                        logger.info("已空闲 {} 秒，停止消费", idleTime);
                        break;
                    }
                    if (idleTime % 5 == 0 && idleTime > 0) {
                        logger.info("等待消息中...已空闲 {} 秒", idleTime);
                    }
                } else {
                    lastMessageTime = System.currentTimeMillis();

                    for (ConsumerRecord<String, String> record : records) {
                        try {
                            String value = record.value();
                            String[] parts = value.split("\t");

                            if (parts.length < 9) {
                                logger.error("消息格式错误 - partition: {}, offset: {}, value: {}",
                                        record.partition(), record.offset(), value);
                                continue;
                            }

                            long seq = Long.parseLong(parts[8]);

                            allConsumedIds.add(seq);
                            consumedIds.add(seq);
                            writer.write(seq + "\n");
                            consumedCount++;

                            if (consumedCount % 50000 == 0) {
                                logger.info("已累计消费: {} 条消息", consumedCount);
                            }

                            if (consumedCount <= 5) {
                                logger.info("消费示例消息 {}: {}", consumedCount, value);
                            }

                        } catch (Exception e) {
                            logger.error("解析消息失败 - partition: {}, offset: {}, value: {}, 错误: {}",
                                    record.partition(), record.offset(), record.value(), e.getMessage());
                        }
                    }
                }
            }

            writer.flush();
            logger.info("消费ID记录文件写入完成");

        } catch (IOException e) {
            logger.error("写入消费记录文件时发生异常", e);
        } finally {
            consumer.close();
            logger.info("Kafka Consumer已关闭");
        }

        long actualDuration = (System.currentTimeMillis() - startTime) / 1000;

        logger.info("========== 消费统计 ==========");
        logger.info("消费总数: {} 条", consumedCount);
        logger.info("消费唯一数: {} 条", consumedIds.size());
        logger.info("消费耗时: {} 秒", actualDuration);
        logger.info("平均消费速率: {} 条/秒", actualDuration > 0 ? consumedCount / actualDuration : 0);
        logger.info("消费ID记录文件: {}", CONSUMED_IDS_FILE);
        logger.info("==============================");

        verify(allConsumedIds, consumedIds);
    }

    private void verify(List<Long> allConsumedIds, Set<Long> uniqueConsumedIds) {
        logger.info("========== 开始数据校验 ==========");

        Set<Long> sentIds = loadSentIds();

        long totalConsumed = allConsumedIds.size();
        long uniqueConsumed = uniqueConsumedIds.size();
        long totalSent = sentIds.size();
        long duplicateCount = totalConsumed - uniqueConsumed;

        logger.info("发送总数: {} 条", totalSent);
        logger.info("消费总数: {} 条", totalConsumed);
        logger.info("消费唯一数: {} 条", uniqueConsumed);
        logger.info("重复总数: {} 条", duplicateCount);

// 查找重复的消息
        if (duplicateCount > 0) {
            logger.warn("========== 检测到重复消息 ==========");
            Map<Long, Integer> countMap = new HashMap<>();
            for (Long id : allConsumedIds) {
                countMap.put(id, countMap.getOrDefault(id, 0) + 1);
            }

            List<Long> duplicateIds = new ArrayList<>();
            for (Map.Entry<Long, Integer> entry : countMap.entrySet()) {
                if (entry.getValue() > 1) {
                    duplicateIds.add(entry.getKey());
                }
            }

            Collections.sort(duplicateIds);
            logger.warn("重复的消息ID种类数: {} 个", duplicateIds.size());

            if (duplicateIds.size() <= 100) {
                logger.warn("所有重复的消息ID: {}", duplicateIds);
            } else {
                logger.warn("重复的消息ID(前100个): {}", duplicateIds.subList(0, 100));
            }

// 显示重复次数详情
            logger.warn("========== 重复次数详情 ==========");
            for (int i = 0; i < Math.min(20, duplicateIds.size()); i++) {
                Long id = duplicateIds.get(i);
                logger.warn("消息ID: {} 重复了 {} 次", id, countMap.get(id));
            }
            logger.warn("===================================");
        } else {
            logger.info("未检测到重复消息");
        }

// 查找丢失的消息
        Set<Long> missingIds = new HashSet<>(sentIds);
        missingIds.removeAll(uniqueConsumedIds);
        long missingCount = missingIds.size();

        logger.info("丢失总数: {} 条", missingCount);

        if (missingCount > 0) {
            logger.error("========== 检测到消息丢失 ==========");
            List<Long> missingList = new ArrayList<>(missingIds);
            Collections.sort(missingList);
            logger.error("丢失的消息ID数量: {} 个", missingList.size());

            if (missingList.size() <= 100) {
                logger.error("所有丢失的消息ID: {}", missingList);
            } else {
                logger.error("丢失的消息ID(前100个): {}", missingList.subList(0, 100));
            }

// 分析丢失消息的分布
            logger.error("========== 丢失消息分布分析 ==========");
            if (missingList.size() > 0) {
                long minMissing = missingList.get(0);
                long maxMissing = missingList.get(missingList.size() - 1);
                logger.error("丢失消息ID范围: {} 到 {}", minMissing, maxMissing);

// 检查是否连续丢失
                List<String> ranges = new ArrayList<>();
                long rangeStart = missingList.get(0);
                long rangeEnd = rangeStart;

                for (int i = 1; i < missingList.size(); i++) {
                    if (missingList.get(i) == rangeEnd + 1) {
                        rangeEnd = missingList.get(i);
                    } else {
                        if (rangeStart == rangeEnd) {
                            ranges.add(String.valueOf(rangeStart));
                        } else {
                            ranges.add(rangeStart + "-" + rangeEnd);
                        }
                        rangeStart = missingList.get(i);
                        rangeEnd = rangeStart;
                    }
                }

                if (rangeStart == rangeEnd) {
                    ranges.add(String.valueOf(rangeStart));
                } else {
                    ranges.add(rangeStart + "-" + rangeEnd);
                }

                logger.error("丢失消息区间数: {}", ranges.size());
                if (ranges.size() <= 20) {
                    logger.error("丢失消息区间: {}", ranges);
                } else {
                    logger.error("丢失消息区间(前20个): {}", ranges.subList(0, 20));
                }
            }
            logger.error("======================================");
        } else {
            logger.info("未检测到消息丢失");
        }

// 查找多余消费的消息（理论上不应该有）
        Set<Long> extraIds = new HashSet<>(uniqueConsumedIds);
        extraIds.removeAll(sentIds);
        long extraCount = extraIds.size();

        if (extraCount > 0) {
            logger.warn("========== 检测到多余消息 ==========");
            List<Long> extraList = new ArrayList<>(extraIds);
            Collections.sort(extraList);
            logger.warn("多余消息ID数量: {} 个", extraList.size());

            if (extraList.size() <= 100) {
                logger.warn("所有多余的消息ID: {}", extraList);
            } else {
                logger.warn("多余的消息ID(前100个): {}", extraList.subList(0, 100));
            }
            logger.warn("===================================");
        }

// 计算准确率
        double accuracy = totalSent > 0 ? (uniqueConsumed - extraCount) * 100.0 / totalSent : 0;
        double duplicateRate = totalConsumed > 0 ? duplicateCount * 100.0 / totalConsumed : 0;
        double lossRate = totalSent > 0 ? missingCount * 100.0 / totalSent : 0;

        logger.info("========== 最终校验结果 ==========");
        logger.info("数据准确率: {}%", String.format("%.4f", accuracy));
        logger.info("消息丢失率: {}%", String.format("%.4f", lossRate));
        logger.info("消息重复率: {}%", String.format("%.4f", duplicateRate));

        if (missingCount == 0 && duplicateCount == 0) {
            logger.info("✓ 数据完整性验证通过：无丢失，无重复");
        } else {
            logger.warn("✗ 数据完整性验证未通过");
        }
        logger.info("===================================");
    }

    private Set<Long> loadSentIds() {
        logger.info("开始加载发送ID记录文件: {}", SENT_IDS_FILE);
        Set<Long> sentIds = new HashSet<>();

        try (BufferedReader reader = new BufferedReader(new FileReader(SENT_IDS_FILE))) {
            String line;
            while ((line = reader.readLine()) != null) {
                try {
                    sentIds.add(Long.parseLong(line.trim()));
                } catch (NumberFormatException e) {
                    logger.error("解析发送ID失败: {}", line);
                }
            }
            logger.info("发送ID记录加载完成，共 {} 条", sentIds.size());
        } catch (IOException e) {
            logger.error("读取发送ID记录文件失败", e);
        }

        return sentIds;
    }

    public static void main(String[] args) {
        Consumer2 consumer = new Consumer2();
        consumer.init();

        int timeout = args.length > 0 ? Integer.parseInt(args[0]) : 30;

        logger.info("启动参数 - 空闲超时: {}秒", timeout);
        consumer.consumeAndVerify(timeout);
    }
}