package com.hao.datacollector.integration.kafka;

import org.apache.kafka.clients.producer.KafkaProducer;
import org.apache.kafka.clients.producer.ProducerConfig;
import org.apache.kafka.clients.producer.ProducerRecord;
import org.apache.kafka.clients.producer.RecordMetadata;
import org.apache.kafka.common.serialization.StringSerializer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.io.FileWriter;
import java.io.IOException;
import java.nio.file.Paths;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Properties;
import java.util.Random;
import java.util.concurrent.Future;
import java.util.concurrent.atomic.AtomicLong;

/**
 * Kafka消息发送压测生产者1号
 *
 * 设计目的：
 * 1. 构造稳定可控的发送流量，用于验证Kafka发送链路稳定性。
 * 2. 记录发送序列ID，便于后续消费端核对丢失与乱序情况。
 *
 * 为什么需要该类：
 * - 压测需要可复现的发送节奏与可追踪的ID记录。
 * - 独立生产者便于与其他测试组隔离验证。
 *
 * 核心实现思路：
 * - 通过单线程定时循环控制QPS与持续时间。
 * - 发送成功后将序列号写入本地日志文件。
 * - 出现异常时记录错误并统计失败数量。
 */
@Component
public class Producer1 {
    private static final Logger logger = LoggerFactory.getLogger(Producer1.class);
    private static final String TOPIC = "ping-topic3";
    private static final String BOOTSTRAP_SERVERS = "192.168.254.2:9092";
    private static final String SENT_IDS_FILE = Paths.get("services", "quant-data-collector", "src", "main",
            "resources", "sent_ids_group1.log").toString();

    private KafkaProducer<String, String> producer;
    private AtomicLong sequenceId = new AtomicLong(0);
    private FileWriter sentIdsWriter;
    private Random random = new Random();
    private SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");

    /**
     * 初始化Kafka生产者与发送ID记录文件
     *
     * 实现逻辑：
     * 1. 构建Kafka生产者配置。
     * 2. 创建Producer实例并初始化ID记录文件。
     * 3. 输出初始化关键信息便于排查。
     *
     * @throws IOException 写入记录文件失败时抛出
     */
    public void init() throws IOException {
        // 实现思路：
        // 1. 初始化生产者配置与连接信息。
        // 2. 创建Producer与记录文件。
        // 3. 打印启动参数用于排查。
        logger.info("初始化Producer1开始|Init_producer1_start");
        logger.info("生产者配置|Producer_config,acks={},enableIdempotence={}", "1", "false");
        logger.info("主题信息|Topic_info,topic={}", TOPIC);
        logger.info("集群地址|Bootstrap_servers,servers={}", BOOTSTRAP_SERVERS);

        Properties props = new Properties();
        props.put(ProducerConfig.BOOTSTRAP_SERVERS_CONFIG, BOOTSTRAP_SERVERS);
        props.put(ProducerConfig.KEY_SERIALIZER_CLASS_CONFIG, StringSerializer.class.getName());
        props.put(ProducerConfig.VALUE_SERIALIZER_CLASS_CONFIG, StringSerializer.class.getName());
        props.put(ProducerConfig.ACKS_CONFIG, "1");
        props.put(ProducerConfig.ENABLE_IDEMPOTENCE_CONFIG, "false");
        props.put(ProducerConfig.MAX_IN_FLIGHT_REQUESTS_PER_CONNECTION, "5");
        props.put(ProducerConfig.BATCH_SIZE_CONFIG, "32768");
        props.put(ProducerConfig.LINGER_MS_CONFIG, "0");
        props.put(ProducerConfig.BUFFER_MEMORY_CONFIG, "67108864");
        props.put(ProducerConfig.COMPRESSION_TYPE_CONFIG, "none");
        props.put(ProducerConfig.REQUEST_TIMEOUT_MS_CONFIG, "30000");
        props.put(ProducerConfig.RETRIES_CONFIG, "0");

        producer = new KafkaProducer<>(props);
        sentIdsWriter = new FileWriter(SENT_IDS_FILE, false);

        logger.info("Producer1初始化完成|Init_producer1_completed");
        logger.info("发送ID记录文件|Sent_id_file,path={}", SENT_IDS_FILE);
        logger.info("初始化结束|Init_finished");
    }

    /**
     * 按指定QPS与持续时间发送消息
     *
     * 实现逻辑：
     * 1. 以秒为单位控制发送节奏。
     * 2. 每条消息生成唯一序列号并写入文件。
     * 3. 统计成功/失败并输出汇总指标。
     *
     * @param qps 每秒发送条数
     * @param durationSeconds 持续时间（秒）
     */
    public void sendMessages(int qps, int durationSeconds) {
        // 实现思路：
        // 1. 用时间窗口控制发送持续时间。
        // 2. 每秒发送qps条消息并flush。
        // 3. 统计成功与失败并输出结果。
        logger.info("发送任务开始|Send_task_start");
        logger.info("发送参数|Send_params,qps={},durationSeconds={}", qps, durationSeconds);
        logger.info("预计发送总数|Expected_total,total={}", qps * durationSeconds);
        logger.info("发送准备完成|Send_prepare_completed");

        long startTime = System.currentTimeMillis();
        long endTime = startTime + (durationSeconds * 1000L);
        long totalSent = 0;
        long totalFailed = 0;

        try {
            int currentSecond = 0;
            while (System.currentTimeMillis() < endTime) {
                long secondStart = System.currentTimeMillis();
                currentSecond++;
                int sentThisSecond = 0;

                for (int i = 0; i < qps; i++) {
                    long seq = sequenceId.incrementAndGet();
                    String message = generateMessage(seq);

                    try {
                        Future<RecordMetadata> future = producer.send(
                                new ProducerRecord<>(TOPIC, String.valueOf(seq), message),
                                (metadata, exception) -> {
                                    if (exception != null) {
                                        logger.error("消息发送失败回调|Send_message_failed_callback,seq={},partition={},offset={},error={}",
                                                seq,
                                                metadata != null ? metadata.partition() : "unknown",
                                                metadata != null ? metadata.offset() : "unknown",
                                                exception.getMessage(),
                                                exception);
                                    }
                                }
                        );

                        sentIdsWriter.write(seq + "\n");
                        totalSent++;
                        sentThisSecond++;

                        if (totalSent % 50000 == 0) {
                            logger.info("累计发送进度|Send_progress,totalSent={}", totalSent);
                        }

                    } catch (Exception e) {
                        logger.error("发送消息异常|Send_message_error,seq={},error={}", seq, e.getMessage(), e);
                        totalFailed++;
                    }
                }

                producer.flush();

                long elapsed = System.currentTimeMillis() - secondStart;
                logger.info("单秒发送统计|Second_send_stats,secondIndex={},sentCount={},costMs={}",
                        currentSecond, sentThisSecond, elapsed);

                if (elapsed < 1000) {
                    try {
                        Thread.sleep(1000 - elapsed);
                    } catch (InterruptedException e) {
                        logger.warn("线程等待中断|Thread_sleep_interrupted", e);
                        break;
                    }
                }
            }

            producer.flush();
            logger.info("最终Flush完成|Final_flush_completed");

        } catch (Exception e) {
            logger.error("发送过程异常|Send_process_error", e);
        } finally {
            cleanup();
        }

        long actualDuration = (System.currentTimeMillis() - startTime) / 1000;
        double actualQps = actualDuration > 0 ? totalSent / (double) actualDuration : 0;

        logger.info("发送统计开始|Send_stats_start");
        logger.info("发送总量|Send_total,totalSent={}", totalSent);
        logger.info("发送成功|Send_success,successCount={}", totalSent - totalFailed);
        logger.info("发送失败|Send_failed,failCount={}", totalFailed);
        logger.info("实际持续时间|Actual_duration,seconds={}", actualDuration);
        logger.info("实际平均QPS|Actual_avg_qps,qps={}", String.format("%.2f", actualQps));
        logger.info("发送成功率|Send_success_rate,ratePercent={}%",
                String.format("%.2f", (totalSent - totalFailed) * 100.0 / totalSent));
        logger.info("发送ID记录文件|Sent_id_file,path={}", SENT_IDS_FILE);
        logger.info("发送统计结束|Send_stats_end");
    }

    /**
     * 生成模拟行情消息体
     *
     * 实现逻辑：
     * 1. 构造证券代码与价格/成交量字段。
     * 2. 生成两列时间戳用于后续校验。
     * 3. 拼接为制表符分隔的发送体。
     *
     * @param seq 消息序列号
     * @return 消息体字符串
     */
    private String generateMessage(long seq) {
        // 实现思路：
        // 1. 使用序列号生成稳定证券代码。
        // 2. 生成随机价格与成交量。
        // 3. 返回固定字段顺序的消息体。
        String symbol = String.format("%06d.SZ", (seq % 1000000) + 1);
        double price = 10.0 + random.nextDouble() * 490.0;
        double volume = 1000.0 + random.nextDouble() * 9000.0;
        String timestamp1 = "2025-09-01 09:25:00";
        String timestamp2 = sdf.format(new Date(System.currentTimeMillis() + random.nextInt(3600000)));

        return String.format("%s\t%s\t%.4f\t%.5f\t%.4f\t1\t%s\t%s\t%d",
                symbol, timestamp1, price, volume, price, timestamp2, timestamp2, seq);
    }

    /**
     * 关闭Producer与发送ID记录文件
     *
     * 实现逻辑：
     * 1. 关闭文件写入器并输出日志。
     * 2. 关闭KafkaProducer释放资源。
     */
    private void cleanup() {
        // 实现思路：
        // 1. 先关闭文件，再关闭Producer。
        // 2. 捕获IO异常并记录。
        try {
            if (sentIdsWriter != null) {
                sentIdsWriter.close();
                logger.info("发送ID记录文件关闭|Sent_id_file_closed");
            }
            if (producer != null) {
                producer.close();
                logger.info("KafkaProducer关闭|Kafka_producer_closed");
            }
        } catch (IOException e) {
            logger.error("资源关闭异常|Resource_close_error", e);
        }
    }

    /**
     * 本地执行入口
     *
     * 实现逻辑：
     * 1. 读取启动参数设置QPS与持续时间。
     * 2. 初始化生产者并开始发送。
     *
     * @param args 启动参数
     * @throws IOException 初始化失败时抛出
     */
    public static void main(String[] args) throws IOException {
        // 实现思路：
        // 1. 参数缺省使用默认QPS与时长。
        // 2. 初始化生产者并执行发送。
        Producer1 producer = new Producer1();
        producer.init();

        int qps = args.length > 0 ? Integer.parseInt(args[0]) : 10000;
        int duration = args.length > 1 ? Integer.parseInt(args[1]) : 60;

        logger.info("启动参数|Startup_params,qps={},durationSeconds={}", qps, duration);
        producer.sendMessages(qps, duration);
    }
}
