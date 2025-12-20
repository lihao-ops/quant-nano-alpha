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
import java.nio.file.Paths;
import java.time.Duration;
import java.util.*;

/**
 * Kafka消费校验工具（消费者2）。
 *
 * 类职责：
 * 1. 从指定Topic消费消息并记录消费ID。
 * 2. 对比发送ID与消费ID，统计丢失、重复与多余数据。
 *
 * 设计目的：
 * - 验证Kafka消费链路完整性与数据一致性。
 *
 * 为什么需要该类：
 * - 需要独立工具对消费链路进行压测与质量核验。
 *
 * 核心实现思路：
 * - 消费消息后提取序号并落盘。
 * - 汇总消费ID与发送ID集合并做差集/交集统计。
 */
@Component
public class Consumer2 {
    private static final Logger logger = LoggerFactory.getLogger(Consumer2.class);
    private static final String TOPIC = "ping-topic3";
    private static final String BOOTSTRAP_SERVERS = "192.168.254.2:9092";
    private static final String GROUP_ID = "test-consumer-group2";
    private static final String CONSUMED_IDS_FILE = Paths.get("services", "quant-data-collector", "src", "main", "resources", "consumed_ids_group2.log").toString();

    private static final String SENT_IDS_FILE = Paths.get("services", "quant-data-collector", "src", "main", "resources", "sent_ids_group2.log").toString();

    private KafkaConsumer<String, String> consumer;

    /**
     * 初始化消费者配置并订阅Topic。
     *
     * 实现逻辑：
     * 1. 组装Kafka配置并创建消费者。
     * 2. 订阅Topic并输出初始化信息。
     */
    public void init() {
        // 实现思路：
        // 1. 记录初始化参数用于排查配置问题。
        // 2. 完成订阅后输出初始化完成日志。
        logger.info("Consumer2初始化开始|Consumer2_init_start");
        logger.info("订阅主题|Topic_subscribe,topic={}", TOPIC);
        logger.info("Kafka地址|Kafka_bootstrap_servers,servers={}", BOOTSTRAP_SERVERS);
        logger.info("消费组|Consumer_group_id,groupId={}", GROUP_ID);

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

        logger.info("Consumer2初始化完成|Consumer2_init_done");
        logger.info("初始化结束|Init_end");
    }

    /**
     * 消费消息并进行完整性校验。
     *
     * 实现逻辑：
     * 1. 循环拉取消息并记录消费ID。
     * 2. 空闲超时后停止消费并落盘ID。
     * 3. 触发校验并输出统计结果。
     *
     * @param timeoutSeconds 空闲超时秒数
     */
    public void consumeAndVerify(int timeoutSeconds) {
        // 实现思路：
        // 1. 持续轮询并记录消费ID与顺序号。
        // 2. 空闲超时后落盘并进行校验统计。
        logger.info("开始消费|Consume_start");
        logger.info("空闲超时秒数|Idle_timeout_seconds,timeoutSeconds={}", timeoutSeconds);
        logger.info("消费阶段开始|Consume_phase_start");

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
                        logger.info("空闲超时停止|Idle_timeout_stop,idleSeconds={}", idleTime);
                        break;
                    }
                    if (idleTime % 5 == 0 && idleTime > 0) {
                        logger.info("等待消息中|Waiting_for_message,idleSeconds={}", idleTime);
                    }
                } else {
                    lastMessageTime = System.currentTimeMillis();

                    for (ConsumerRecord<String, String> record : records) {
                        try {
                            String value = record.value();
                            String[] parts = value.split("\t");

                            if (parts.length < 9) {
                                int valueLength = value == null ? 0 : value.length();
                                logger.warn("消息格式错误|Message_format_invalid,partition={},offset={},valueLength={}",
                                        record.partition(), record.offset(), valueLength);
                                continue;
                            }

                            long seq = Long.parseLong(parts[8]);

                            allConsumedIds.add(seq);
                            consumedIds.add(seq);
                            writer.write(seq + "\n");
                            consumedCount++;

                            if (consumedCount % 50000 == 0) {
                                logger.info("累计消费|Consumed_total,count={}", consumedCount);
                            }

                            if (consumedCount <= 5) {
                                logger.info("消费示例|Consume_sample,index={},value={}", consumedCount, value);
                            }

                        } catch (Exception e) {
                            int valueLength = record.value() == null ? 0 : record.value().length();
                            logger.error("解析消息失败|Parse_message_failed,partition={},offset={},valueLength={}",
                                    record.partition(), record.offset(), valueLength, e);
                        }
                    }
                }
            }

            writer.flush();
            logger.info("消费ID写入完成|Consumed_id_write_done");

        } catch (IOException e) {
            logger.error("消费ID写入异常|Consumed_id_write_error", e);
        } finally {
            consumer.close();
            logger.info("Consumer已关闭|Consumer_closed");
        }

        long actualDuration = (System.currentTimeMillis() - startTime) / 1000;

        logger.info("消费统计开始|Consume_stats_start");
        logger.info("消费总数|Consumed_total,count={}", consumedCount);
        logger.info("消费唯一数|Consumed_unique_count,count={}", consumedIds.size());
        logger.info("消费耗时秒数|Consume_duration_seconds,seconds={}", actualDuration);
        logger.info("平均消费速率|Average_consume_rate,ratePerSecond={}", actualDuration > 0 ? consumedCount / actualDuration : 0);
        logger.info("消费ID文件|Consumed_id_file,path={}", CONSUMED_IDS_FILE);
        logger.info("消费统计结束|Consume_stats_end");

        verify(allConsumedIds, consumedIds);
    }

    /**
     * 校验消费与发送ID，统计重复、丢失与多余消息。
     *
     * 实现逻辑：
     * 1. 加载发送ID集合并计算差集。
     * 2. 输出重复、丢失与多余统计结果。
     *
     * @param allConsumedIds    全部消费ID列表
     * @param uniqueConsumedIds 去重后的消费ID集合
     */
    private void verify(List<Long> allConsumedIds, Set<Long> uniqueConsumedIds) {
        // 实现思路：
        // 1. 计算重复与丢失集合。
        // 2. 输出校验统计与异常分布。
        logger.info("校验开始|Verify_start");

        Set<Long> sentIds = loadSentIds();

        long totalConsumed = allConsumedIds.size();
        long uniqueConsumed = uniqueConsumedIds.size();
        long totalSent = sentIds.size();
        long duplicateCount = totalConsumed - uniqueConsumed;

        logger.info("发送总数|Sent_total,count={}", totalSent);
        logger.info("消费总数|Consumed_total,count={}", totalConsumed);
        logger.info("消费唯一数|Consumed_unique_count,count={}", uniqueConsumed);
        logger.info("重复总数|Duplicate_total,count={}", duplicateCount);

// 查找重复的消息
        if (duplicateCount > 0) {
            logger.warn("检测到重复消息|Duplicate_message_detected");
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
            logger.warn("重复ID种类数|Duplicate_id_type_count,count={}", duplicateIds.size());

            if (duplicateIds.size() <= 100) {
                logger.warn("重复ID列表|Duplicate_id_list,ids={}", duplicateIds);
            } else {
                logger.warn("重复ID列表前100|Duplicate_id_list_top100,ids={}", duplicateIds.subList(0, 100));
            }

// 显示重复次数详情
            logger.warn("重复次数详情|Duplicate_count_detail");
            for (int i = 0; i < Math.min(20, duplicateIds.size()); i++) {
                Long id = duplicateIds.get(i);
                logger.warn("消息重复次数|Message_duplicate_count,id={},count={}", id, countMap.get(id));
            }
            logger.warn("重复统计结束|Duplicate_stats_end");
        } else {
            logger.info("未检测到重复消息|No_duplicate_message");
        }

// 查找丢失的消息
        Set<Long> missingIds = new HashSet<>(sentIds);
        missingIds.removeAll(uniqueConsumedIds);
        long missingCount = missingIds.size();

        logger.info("丢失总数|Missing_total,count={}", missingCount);

        if (missingCount > 0) {
            logger.warn("检测到消息丢失|Missing_message_detected");
            List<Long> missingList = new ArrayList<>(missingIds);
            Collections.sort(missingList);
            logger.warn("丢失ID数量|Missing_id_count,count={}", missingList.size());

            if (missingList.size() <= 100) {
                logger.warn("丢失ID列表|Missing_id_list,ids={}", missingList);
            } else {
                logger.warn("丢失ID列表前100|Missing_id_list_top100,ids={}", missingList.subList(0, 100));
            }

// 分析丢失消息的分布
            logger.warn("丢失分布分析|Missing_distribution_analysis");
            if (missingList.size() > 0) {
                long minMissing = missingList.get(0);
                long maxMissing = missingList.get(missingList.size() - 1);
                logger.warn("丢失ID范围|Missing_id_range,minId={},maxId={}", minMissing, maxMissing);

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

                logger.warn("丢失区间数|Missing_range_count,count={}", ranges.size());
                if (ranges.size() <= 20) {
                    logger.warn("丢失区间列表|Missing_range_list,ranges={}", ranges);
                } else {
                    logger.warn("丢失区间前20|Missing_range_list_top20,ranges={}", ranges.subList(0, 20));
                }
            }
            logger.warn("丢失统计结束|Missing_stats_end");
        } else {
            logger.info("未检测到消息丢失|No_missing_message");
        }

// 查找多余消费的消息（理论上不应该有）
        Set<Long> extraIds = new HashSet<>(uniqueConsumedIds);
        extraIds.removeAll(sentIds);
        long extraCount = extraIds.size();

        if (extraCount > 0) {
            logger.warn("检测到多余消息|Extra_message_detected");
            List<Long> extraList = new ArrayList<>(extraIds);
            Collections.sort(extraList);
            logger.warn("多余ID数量|Extra_id_count,count={}", extraList.size());

            if (extraList.size() <= 100) {
                logger.warn("多余ID列表|Extra_id_list,ids={}", extraList);
            } else {
                logger.warn("多余ID列表前100|Extra_id_list_top100,ids={}", extraList.subList(0, 100));
            }
            logger.warn("多余统计结束|Extra_stats_end");
        }

// 计算准确率
        double accuracy = totalSent > 0 ? (uniqueConsumed - extraCount) * 100.0 / totalSent : 0;
        double duplicateRate = totalConsumed > 0 ? duplicateCount * 100.0 / totalConsumed : 0;
        double lossRate = totalSent > 0 ? missingCount * 100.0 / totalSent : 0;

        logger.info("校验结果开始|Verify_result_start");
        logger.info("数据准确率|Accuracy_percent,value={}", String.format("%.4f", accuracy));
        logger.info("消息丢失率|Missing_rate_percent,value={}", String.format("%.4f", lossRate));
        logger.info("消息重复率|Duplicate_rate_percent,value={}", String.format("%.4f", duplicateRate));

        if (missingCount == 0 && duplicateCount == 0) {
            logger.info("完整性验证通过|Integrity_check_passed");
        } else {
            logger.warn("完整性验证未通过|Integrity_check_failed");
        }
        logger.info("校验结果结束|Verify_result_end");
    }

    /**
     * 加载发送ID记录文件。
     *
     * 实现逻辑：
     * 1. 逐行读取并解析为Long。
     * 2. 过滤非法行并记录告警。
     *
     * @return 发送ID集合
     */
    private Set<Long> loadSentIds() {
        // 实现思路：
        // 1. 读取文件并解析为Long集合。
        // 2. 对非法行记录告警并跳过。
        logger.info("加载发送ID文件|Load_sent_id_file,path={}", SENT_IDS_FILE);
        Set<Long> sentIds = new HashSet<>();

        try (BufferedReader reader = new BufferedReader(new FileReader(SENT_IDS_FILE))) {
            String line;
            while ((line = reader.readLine()) != null) {
                try {
                    sentIds.add(Long.parseLong(line.trim()));
                } catch (NumberFormatException e) {
                    logger.warn("解析发送ID失败|Parse_sent_id_failed,line={}", line);
                }
            }
            logger.info("发送ID加载完成|Sent_id_load_done,count={}", sentIds.size());
        } catch (IOException e) {
            logger.error("读取发送ID文件失败|Read_sent_id_failed", e);
        }

        return sentIds;
    }

    /**
     * 启动入口，便于本地手工验证。
     *
     * 实现逻辑：
     * 1. 初始化消费者。
     * 2. 读取参数并开始消费校验。
     *
     * @param args 启动参数
     */
    public static void main(String[] args) {
        // 实现思路：
        // 1. 初始化消费者并解析参数。
        // 2. 启动消费与校验流程。
        Consumer2 consumer = new Consumer2();
        consumer.init();

        int timeout = args.length > 0 ? Integer.parseInt(args[0]) : 30;

        logger.info("启动参数|Startup_args,timeoutSeconds={}", timeout);
        consumer.consumeAndVerify(timeout);
    }
}
