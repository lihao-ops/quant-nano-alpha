package com.hao.datacollector.integration.kafka;

// 第二组：开启ack=all + 开启幂等

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
 * Kafka生产压测工具（生产者2）。
 *
 * 类职责：
 * 1. 按指定QPS发送消息并记录发送ID。
 * 2. 输出发送统计并提供回调失败信息。
 *
 * 设计目的：
 * - 验证Kafka生产端吞吐与幂等配置效果。
 *
 * 为什么需要该类：
 * - 需要独立工具对生产链路进行压测与一致性验证。
 *
 * 核心实现思路：
 * - 构造带序号的消息并按QPS发送。
 * - 落盘发送ID并输出统计结果。
 */
@Component
public class Producer2 {
    private static final Logger logger = LoggerFactory.getLogger(Producer2.class);
    private static final String TOPIC = "ping-topic3";
    private static final String BOOTSTRAP_SERVERS = "192.168.254.2:9092";
    private static final String SENT_IDS_FILE = Paths.get("services", "quant-data-collector", "src", "main", "resources", "sent_ids_group2.log").toString();
    
    private KafkaProducer<String, String> producer;
    private AtomicLong sequenceId = new AtomicLong(0);
    private FileWriter sentIdsWriter;
    private Random random = new Random();
    private SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");

    /**
     * 初始化Producer配置与ID记录文件。
     *
     * 实现逻辑：
     * 1. 构建Producer配置并创建实例。
     * 2. 初始化ID落盘文件并输出配置摘要。
     */
    public void init() throws IOException {
        // 实现思路：
        // 1. 创建Producer并准备发送配置。
        // 2. 打开发送ID记录文件用于落盘。
        logger.info("Producer2初始化开始|Producer2_init_start");
        logger.info("发送配置|Producer_config,acks=all,idempotence=true");
        logger.info("发送主题|Topic_send,topic={}", TOPIC);
        logger.info("Kafka地址|Kafka_bootstrap_servers,servers={}", BOOTSTRAP_SERVERS);
        
        Properties props = new Properties();
        props.put(ProducerConfig.BOOTSTRAP_SERVERS_CONFIG, BOOTSTRAP_SERVERS);
        props.put(ProducerConfig.KEY_SERIALIZER_CLASS_CONFIG, StringSerializer.class.getName());
        props.put(ProducerConfig.VALUE_SERIALIZER_CLASS_CONFIG, StringSerializer.class.getName());
        props.put(ProducerConfig.ACKS_CONFIG, "all");
        props.put(ProducerConfig.ENABLE_IDEMPOTENCE_CONFIG, "true");
        props.put(ProducerConfig.MAX_IN_FLIGHT_REQUESTS_PER_CONNECTION, "5");
        props.put(ProducerConfig.BATCH_SIZE_CONFIG, "32768");
        props.put(ProducerConfig.LINGER_MS_CONFIG, "0");
        props.put(ProducerConfig.BUFFER_MEMORY_CONFIG, "67108864");
        props.put(ProducerConfig.COMPRESSION_TYPE_CONFIG, "none");
        props.put(ProducerConfig.REQUEST_TIMEOUT_MS_CONFIG, "30000");
        props.put(ProducerConfig.RETRIES_CONFIG, Integer.MAX_VALUE);
        props.put(ProducerConfig.MAX_BLOCK_MS_CONFIG, "60000");
        
        producer = new KafkaProducer<>(props);
        sentIdsWriter = new FileWriter(SENT_IDS_FILE, false);
        
        logger.info("Producer2初始化完成|Producer2_init_done");
        logger.info("发送ID文件|Sent_id_file,path={}", SENT_IDS_FILE);
        logger.info("初始化结束|Init_end");
    }

    /**
     * 按QPS发送消息并记录统计。
     *
     * 实现逻辑：
     * 1. 按秒控制发送速率并落盘ID。
     * 2. 记录发送结果与失败统计。
     *
     * @param qps             目标每秒发送数
     * @param durationSeconds 持续时间秒数
     */
    public void sendMessages(int qps, int durationSeconds) {
        // 实现思路：
        // 1. 按秒控制发送速率并落盘ID。
        // 2. 累计统计发送成功与失败数量。
        logger.info("开始发送|Send_start");
        logger.info("目标QPS|Target_qps,qps={}", qps);
        logger.info("持续时间秒数|Duration_seconds,seconds={}", durationSeconds);
        logger.info("预计发送总数|Expected_total,count={}", qps * durationSeconds);
        logger.info("发送阶段开始|Send_phase_start");
        
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
                                    int partition = metadata == null ? -1 : metadata.partition();
                                    long offset = metadata == null ? -1L : metadata.offset();
                                    logger.error("消息发送回调失败|Send_callback_failed,seq={},partition={},offset={}",
                                        seq,
                                        partition,
                                        offset,
                                        exception);
                                }
                            }
                        );
                        
                        sentIdsWriter.write(seq + "\n");
                        totalSent++;
                        sentThisSecond++;
                        
                        if (totalSent % 50000 == 0) {
                            logger.info("累计发送|Sent_total,count={}", totalSent);
                        }
                        
                    } catch (Exception e) {
                        logger.error("发送消息异常|Send_message_error,seq={}", seq, e);
                        totalFailed++;
                    }
                }
                
                producer.flush();
                
                long elapsed = System.currentTimeMillis() - secondStart;
                logger.info("秒级发送统计|Second_send_stats,second={},count={},elapsedMs={}", currentSecond, sentThisSecond, elapsed);
                
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
            logger.info("最终Flush完成|Final_flush_done");
            
        } catch (Exception e) {
            logger.error("发送过程异常|Send_process_error", e);
        } finally {
            cleanup();
        }

        long actualDuration = (System.currentTimeMillis() - startTime) / 1000;
        double actualQps = actualDuration > 0 ? totalSent / (double) actualDuration : 0;
        
        logger.info("发送统计开始|Send_stats_start");
        logger.info("发送总数|Sent_total,count={}", totalSent);
        logger.info("发送成功|Sent_success,count={}", totalSent - totalFailed);
        logger.info("发送失败|Sent_failed,count={}", totalFailed);
        logger.info("实际耗时秒数|Actual_duration_seconds,seconds={}", actualDuration);
        logger.info("实际平均QPS|Actual_avg_qps,value={}", String.format("%.2f", actualQps));
        logger.info("发送成功率|Send_success_rate,value={}", String.format("%.2f", (totalSent - totalFailed) * 100.0 / totalSent));
        logger.info("发送ID文件|Sent_id_file,path={}", SENT_IDS_FILE);
        logger.info("发送统计结束|Send_stats_end");
    }

    /**
     * 生成带序号的消息体。
     *
     * 实现逻辑：
     * 1. 生成随机行情字段与时间戳。
     * 2. 拼接为制表符分隔的消息体。
     *
     * @param seq 序号
     * @return 消息体
     */
    private String generateMessage(long seq) {
        // 实现思路：
        // 1. 构造行情字段。
        // 2. 组装为Tab分隔字符串。
        String symbol = String.format("%06d.SZ", (seq % 1000000) + 1);
        double price = 10.0 + random.nextDouble() * 490.0;
        double volume = 1000.0 + random.nextDouble() * 9000.0;
        String timestamp1 = "2025-09-01 09:25:00";
        String timestamp2 = sdf.format(new Date(System.currentTimeMillis() + random.nextInt(3600000)));
        
        return String.format("%s\t%s\t%.4f\t%.5f\t%.4f\t1\t%s\t%s\t%d",
            symbol, timestamp1, price, volume, price, timestamp2, timestamp2, seq);
    }

    /**
     * 释放资源并输出关闭日志。
     *
     * 实现逻辑：
     * 1. 关闭ID记录文件。
     * 2. 关闭Kafka Producer。
     */
    private void cleanup() {
        // 实现思路：
        // 1. 按顺序释放文件与Producer资源。
        // 2. 捕获并记录关闭异常。
        try {
            if (sentIdsWriter != null) {
                sentIdsWriter.close();
                logger.info("发送ID文件关闭|Sent_id_file_closed");
            }
            if (producer != null) {
                producer.close();
                logger.info("Producer已关闭|Producer_closed");
            }
        } catch (IOException e) {
            logger.error("资源关闭异常|Resource_close_error", e);
        }
    }

    /**
     * 启动入口，便于本地手工验证。
     *
     * 实现逻辑：
     * 1. 初始化Producer。
     * 2. 读取参数并开始发送。
     *
     * @param args 启动参数
     */
    public static void main(String[] args) throws IOException {
        // 实现思路：
        // 1. 初始化Producer并解析参数。
        // 2. 启动发送流程。
        Producer2 producer = new Producer2();
        producer.init();
        
        int qps = args.length > 0 ? Integer.parseInt(args[0]) : 10000;
        int duration = args.length > 1 ? Integer.parseInt(args[1]) : 60;
        
        logger.info("启动参数|Startup_args,qps={},durationSeconds={}", qps, duration);
        producer.sendMessages(qps, duration);
    }
}
