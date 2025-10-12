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
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Properties;
import java.util.Random;
import java.util.concurrent.Future;
import java.util.concurrent.atomic.AtomicLong;

@Component
public class Producer2 {
    private static final Logger logger = LoggerFactory.getLogger(Producer2.class);
    private static final String TOPIC = "ping-topic3";
    private static final String BOOTSTRAP_SERVERS = "192.168.254.2:9092";
    private static final String SENT_IDS_FILE = "E:\\项目暂存\\quant-nano-alpha\\services\\quant-data-collector\\src\\main\\resources\\sent_ids_group2.log";
    
    private KafkaProducer<String, String> producer;
    private AtomicLong sequenceId = new AtomicLong(0);
    private FileWriter sentIdsWriter;
    private Random random = new Random();
    private SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");

    public void init() throws IOException {
        logger.info("========== 初始化Producer2 ==========");
        logger.info("配置: acks=all, enable.idempotence=true");
        logger.info("Topic: {}", TOPIC);
        logger.info("Bootstrap Servers: {}", BOOTSTRAP_SERVERS);
        
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
        
        logger.info("Producer2初始化完成");
        logger.info("发送ID记录文件: {}", SENT_IDS_FILE);
        logger.info("=====================================");
    }

    public void sendMessages(int qps, int durationSeconds) {
        logger.info("========== 开始发送消息 ==========");
        logger.info("目标QPS: {} 条/秒", qps);
        logger.info("持续时间: {} 秒", durationSeconds);
        logger.info("预计发送总数: {} 条", qps * durationSeconds);
        logger.info("=================================");
        
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
                                    logger.error("消息发送失败回调 - seq: {}, 分区: {}, offset: {}, 异常: {}", 
                                        seq, 
                                        metadata != null ? metadata.partition() : "unknown",
                                        metadata != null ? metadata.offset() : "unknown",
                                        exception.getMessage());
                                }
                            }
                        );
                        
                        sentIdsWriter.write(seq + "\n");
                        totalSent++;
                        sentThisSecond++;
                        
                        if (totalSent % 50000 == 0) {
                            logger.info("已累计发送: {} 条消息", totalSent);
                        }
                        
                    } catch (Exception e) {
                        logger.error("发送消息异常 - seq: {}, 错误信息: {}", seq, e.getMessage());
                        totalFailed++;
                    }
                }
                
                producer.flush();
                
                long elapsed = System.currentTimeMillis() - secondStart;
                logger.info("第 {} 秒: 发送 {} 条消息, 耗时 {} ms", currentSecond, sentThisSecond, elapsed);
                
                if (elapsed < 1000) {
                    try {
                        Thread.sleep(1000 - elapsed);
                    } catch (InterruptedException e) {
                        logger.warn("线程等待被中断", e);
                        break;
                    }
                }
            }
            
            producer.flush();
            logger.info("最终flush完成");
            
        } catch (Exception e) {
            logger.error("发送过程发生异常", e);
        } finally {
            cleanup();
        }

        long actualDuration = (System.currentTimeMillis() - startTime) / 1000;
        double actualQps = actualDuration > 0 ? totalSent / (double) actualDuration : 0;
        
        logger.info("========== Producer2发送统计 ==========");
        logger.info("总发送数: {} 条", totalSent);
        logger.info("发送成功: {} 条", totalSent - totalFailed);
        logger.info("发送失败: {} 条", totalFailed);
        logger.info("实际持续时间: {} 秒", actualDuration);
        logger.info("实际平均QPS: {}", String.format("%.2f", actualQps));
        logger.info("发送成功率: {}%", String.format("%.2f", (totalSent - totalFailed) * 100.0 / totalSent));
        logger.info("发送ID记录文件: {}", SENT_IDS_FILE);
        logger.info("======================================");
    }

    private String generateMessage(long seq) {
        String symbol = String.format("%06d.SZ", (seq % 1000000) + 1);
        double price = 10.0 + random.nextDouble() * 490.0;
        double volume = 1000.0 + random.nextDouble() * 9000.0;
        String timestamp1 = "2025-09-01 09:25:00";
        String timestamp2 = sdf.format(new Date(System.currentTimeMillis() + random.nextInt(3600000)));
        
        return String.format("%s\t%s\t%.4f\t%.5f\t%.4f\t1\t%s\t%s\t%d",
            symbol, timestamp1, price, volume, price, timestamp2, timestamp2, seq);
    }

    private void cleanup() {
        try {
            if (sentIdsWriter != null) {
                sentIdsWriter.close();
                logger.info("发送ID记录文件已关闭");
            }
            if (producer != null) {
                producer.close();
                logger.info("Kafka Producer已关闭");
            }
        } catch (IOException e) {
            logger.error("关闭资源时发生异常", e);
        }
    }

    public static void main(String[] args) throws IOException {
        Producer2 producer = new Producer2();
        producer.init();
        
        int qps = args.length > 0 ? Integer.parseInt(args[0]) : 10000;
        int duration = args.length > 1 ? Integer.parseInt(args[1]) : 60;
        
        logger.info("启动参数 - QPS: {}, Duration: {}秒", qps, duration);
        producer.sendMessages(qps, duration);
    }
}
