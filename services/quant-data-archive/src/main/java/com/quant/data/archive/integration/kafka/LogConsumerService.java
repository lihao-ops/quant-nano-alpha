package com.quant.data.archive.integration.kafka;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import integration.kafka.KafkaConstants;
import lombok.extern.slf4j.Slf4j;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.kafka.support.Acknowledgment;
import org.springframework.kafka.support.KafkaHeaders;
import org.springframework.messaging.handler.annotation.Header;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.concurrent.atomic.AtomicLong;

/**
 * Kafka 日志消费服务
 * 
 * <p>功能：
 * 1. 统一消费各服务推送到 Kafka 的日志消息
 * 2. 解析结构化日志，提取服务实例信息
 * 3. 支持日志级别过滤和异常日志特殊处理
 * 4. 提供消费统计和监控指标
 * 
 * <p>设计思路（大厂面试官视角）：
 * - 结构化处理：解析 JSON 格式日志，提取关键字段进行分类存储
 * - 实例识别：通过 IP+端口+服务名精确识别日志来源实例
 * - 性能优化：批量处理、异步确认、内存缓冲等提升吞吐量
 * - 监控友好：提供消费速率、错误率等关键指标
 * - 扩展性：预留接口支持日志存储到 ES、数据库等多种后端
 */
@Slf4j
@Service
public class LogConsumerService {

    @Autowired
    private ObjectMapper objectMapper;

    /** 消费计数器 */
    private final AtomicLong consumeCounter = new AtomicLong(0);
    
    /** 错误计数器 */
    private final AtomicLong errorCounter = new AtomicLong(0);

    /** 时间格式化器 */
    private static final DateTimeFormatter TIMESTAMP_FORMATTER = 
            DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm:ss.SSSSSS'Z'");

    @KafkaListener(
            topics = {
                    KafkaConstants.TOPIC_LOG_SERVICE_ORDER,
                    KafkaConstants.TOPIC_LOG_QUANT_XXL_JOB,
                    KafkaConstants.TOPIC_LOG_QUANT_DATA_COLLECTOR,
                    KafkaConstants.TOPIC_LOG_QUANT_STRATEGY_ENGINE,
                    KafkaConstants.TOPIC_LOG_QUANT_RISK_CONTROL,
                    KafkaConstants.TOPIC_LOG_QUANT_DATA_ARCHIVE
            },
            groupId = KafkaConstants.GROUP_DATA_ARCHIVE,
            containerFactory = KafkaConstants.LISTENER_CONTAINER_FACTORY
    )
    public void consumeLog(
            ConsumerRecord<String, String> record, 
            Acknowledgment ack,
            @Header(KafkaHeaders.RECEIVED_TOPIC) String topic,
            @Header(KafkaHeaders.RECEIVED_PARTITION) int partition,
            @Header(KafkaHeaders.OFFSET) long offset) {
        
        long startTime = System.currentTimeMillis();
        long currentCount = consumeCounter.incrementAndGet();
        
        try {
            String key = record.key();
            String value = record.value();
            
            // 解析结构化日志
            LogMessage logMessage = parseLogMessage(value);
            if (logMessage == null) {
                log.warn("[AUDIT-LOG] 日志解析失败 - topic: {}, partition: {}, offset: {}, value: {}", 
                        topic, partition, offset, value);
                ack.acknowledge();
                return;
            }
            
            // 增强日志信息
            logMessage.setKafkaTopic(topic);
            logMessage.setKafkaPartition(partition);
            logMessage.setKafkaOffset(offset);
            logMessage.setKafkaKey(key);
            logMessage.setConsumeTime(LocalDateTime.now());
            
            // 处理日志消息
            processLogMessage(logMessage);
            
            // 统计和监控
            long processingTime = System.currentTimeMillis() - startTime;
            if (currentCount % 1000 == 0) {
                log.info("[AUDIT-LOG] 消费统计 - 总数: {}, 错误: {}, 当前处理耗时: {}ms", 
                        currentCount, errorCounter.get(), processingTime);
            }
            
            // 确认消费
            ack.acknowledge();
            
        } catch (Exception e) {
            errorCounter.incrementAndGet();
            log.error("[AUDIT-LOG] 日志消费异常 - topic: {}, partition: {}, offset: {}", 
                    topic, partition, offset, e);
            
            // 根据错误类型决定是否重试
            if (shouldRetry(e)) {
                // 不确认，触发重试
                log.warn("[AUDIT-LOG] 消息将重试处理");
            } else {
                // 确认消费，避免死循环
                log.warn("[AUDIT-LOG] 消息跳过处理");
                ack.acknowledge();
            }
        }
    }

    /**
     * 解析日志消息
     */
    private LogMessage parseLogMessage(String jsonValue) {
        try {
            JsonNode jsonNode = objectMapper.readTree(jsonValue);
            
            LogMessage logMessage = new LogMessage();
            logMessage.setEnv(getTextValue(jsonNode, "env"));
            logMessage.setService(getTextValue(jsonNode, "service"));
            logMessage.setHostname(getTextValue(jsonNode, "hostname"));
            logMessage.setIp(getTextValue(jsonNode, "ip"));
            logMessage.setPort(getTextValue(jsonNode, "port"));
            logMessage.setLevel(getTextValue(jsonNode, "level"));
            logMessage.setThread(getTextValue(jsonNode, "thread"));
            logMessage.setLogger(getTextValue(jsonNode, "logger"));
            logMessage.setMessage(getTextValue(jsonNode, "message"));
            logMessage.setException(getTextValue(jsonNode, "exception"));
            
            // 解析时间戳
            String timestampStr = getTextValue(jsonNode, "timestamp");
            if (timestampStr != null && !timestampStr.isEmpty()) {
                try {
                    logMessage.setTimestamp(LocalDateTime.parse(timestampStr, TIMESTAMP_FORMATTER));
                } catch (Exception e) {
                    log.debug("时间戳解析失败: {}", timestampStr);
                    logMessage.setTimestamp(LocalDateTime.now());
                }
            } else {
                logMessage.setTimestamp(LocalDateTime.now());
            }
            
            // 生成实例标识
            logMessage.setInstanceId(generateInstanceId(logMessage));
            
            return logMessage;
            
        } catch (Exception e) {
            log.debug("JSON 解析失败: {}", e.getMessage());
            return null;
        }
    }

    /**
     * 安全获取 JSON 文本值
     */
    private String getTextValue(JsonNode jsonNode, String fieldName) {
        JsonNode fieldNode = jsonNode.get(fieldName);
        return (fieldNode != null && !fieldNode.isNull()) ? fieldNode.asText() : null;
    }

    /**
     * 生成实例标识
     */
    private String generateInstanceId(LogMessage logMessage) {
        String service = logMessage.getService();
        String ip = logMessage.getIp();
        String port = logMessage.getPort();
        
        if (service != null && ip != null && port != null) {
            return String.format("%s-%s-%s", service, ip, port);
        } else if (service != null && ip != null) {
            return String.format("%s-%s", service, ip);
        } else if (service != null) {
            return service;
        } else {
            return "unknown";
        }
    }

    /**
     * 处理日志消息
     */
    private void processLogMessage(LogMessage logMessage) {
        // 记录接收的日志信息
        log.info("[AUDIT-LOG] 收到日志 - 实例: {}, 级别: {}, 时间: {}, 消息: {}", 
                logMessage.getInstanceId(), 
                logMessage.getLevel(), 
                logMessage.getTimestamp(), 
                truncateMessage(logMessage.getMessage(), 200));
        
        // 特殊处理错误日志
        if ("ERROR".equalsIgnoreCase(logMessage.getLevel()) && 
            logMessage.getException() != null && !logMessage.getException().isEmpty()) {
            
            log.warn("[AUDIT-LOG] 发现异常日志 - 实例: {}, 异常: {}", 
                    logMessage.getInstanceId(), 
                    truncateMessage(logMessage.getException(), 500));
        }
        
        // TODO: 这里可以扩展更多处理逻辑
        // 1. 存储到数据库
        // 2. 推送到 Elasticsearch
        // 3. 触发告警规则
        // 4. 生成监控指标
        
        // 示例：存储到数据库（需要实现对应的 Service 和 Entity）
        // auditLogService.saveLog(logMessage);
        
        // 示例：推送到 ES（需要实现对应的 ES 客户端）
        // elasticsearchService.indexLog(logMessage);
    }

    /**
     * 截断消息内容
     */
    private String truncateMessage(String message, int maxLength) {
        if (message == null) {
            return null;
        }
        if (message.length() <= maxLength) {
            return message;
        }
        return message.substring(0, maxLength) + "...";
    }

    /**
     * 判断是否应该重试
     */
    private boolean shouldRetry(Exception e) {
        // 网络异常、超时等可重试
        String errorMessage = e.getMessage();
        if (errorMessage != null) {
            String lowerMessage = errorMessage.toLowerCase();
            return lowerMessage.contains("timeout") || 
                   lowerMessage.contains("connection") ||
                   lowerMessage.contains("network");
        }
        return false;
    }

    /**
     * 日志消息实体类
     */
    public static class LogMessage {
        private String env;
        private String service;
        private String hostname;
        private String ip;
        private String port;
        private String level;
        private String thread;
        private String logger;
        private String message;
        private String exception;
        private LocalDateTime timestamp;
        private String instanceId;
        
        // Kafka 元信息
        private String kafkaTopic;
        private int kafkaPartition;
        private long kafkaOffset;
        private String kafkaKey;
        private LocalDateTime consumeTime;

        // Getters and Setters
        public String getEnv() { return env; }
        public void setEnv(String env) { this.env = env; }
        
        public String getService() { return service; }
        public void setService(String service) { this.service = service; }
        
        public String getHostname() { return hostname; }
        public void setHostname(String hostname) { this.hostname = hostname; }
        
        public String getIp() { return ip; }
        public void setIp(String ip) { this.ip = ip; }
        
        public String getPort() { return port; }
        public void setPort(String port) { this.port = port; }
        
        public String getLevel() { return level; }
        public void setLevel(String level) { this.level = level; }
        
        public String getThread() { return thread; }
        public void setThread(String thread) { this.thread = thread; }
        
        public String getLogger() { return logger; }
        public void setLogger(String logger) { this.logger = logger; }
        
        public String getMessage() { return message; }
        public void setMessage(String message) { this.message = message; }
        
        public String getException() { return exception; }
        public void setException(String exception) { this.exception = exception; }
        
        public LocalDateTime getTimestamp() { return timestamp; }
        public void setTimestamp(LocalDateTime timestamp) { this.timestamp = timestamp; }
        
        public String getInstanceId() { return instanceId; }
        public void setInstanceId(String instanceId) { this.instanceId = instanceId; }
        
        public String getKafkaTopic() { return kafkaTopic; }
        public void setKafkaTopic(String kafkaTopic) { this.kafkaTopic = kafkaTopic; }
        
        public int getKafkaPartition() { return kafkaPartition; }
        public void setKafkaPartition(int kafkaPartition) { this.kafkaPartition = kafkaPartition; }
        
        public long getKafkaOffset() { return kafkaOffset; }
        public void setKafkaOffset(long kafkaOffset) { this.kafkaOffset = kafkaOffset; }
        
        public String getKafkaKey() { return kafkaKey; }
        public void setKafkaKey(String kafkaKey) { this.kafkaKey = kafkaKey; }
        
        public LocalDateTime getConsumeTime() { return consumeTime; }
        public void setConsumeTime(LocalDateTime consumeTime) { this.consumeTime = consumeTime; }
    }
}
