package com.hao.datacollector.web.config;

import lombok.Data;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

import jakarta.annotation.PostConstruct;

/**
 * Kafka 日志配置类
 * 
 * <p>功能：
 * 1. 通过 @Value 注解动态配置 Kafka 日志推送参数
 * 2. 支持运行时调整日志级别和推送策略
 * 3. 提供配置验证和默认值设置
 * 
 * <p>设计思路（大厂面试官视角）：
 * - 配置外部化：所有配置项可通过 application.yml 动态调整，无需重新编译
 * - 参数验证：启动时验证配置合法性，避免运行时异常
 * - 性能优化：支持批量发送、压缩、异步等性能参数配置
 * - 监控友好：提供配置项日志输出，便于运维排查
 * - 环境隔离：不同环境可使用不同的日志推送策略
 */
@Slf4j
@Data
@Configuration
@ConfigurationProperties(prefix = "logging.kafka")
public class KafkaLogConfig {

    /** Kafka 日志推送是否启用 */
    @Value("${logging.kafka.enabled:true}")
    private boolean enabled;

    /** 推送到 Kafka 的最低日志级别 */
    @Value("${logging.kafka.level:INFO}")
    private String level;

    /** Kafka 主题名称 */
    @Value("${logging.kafka.topic:log-quant-data-collector}")
    private String topic;

    /** Kafka 集群地址 */
    @Value("${spring.kafka.bootstrap-servers:localhost:9092}")
    private String bootstrapServers;

    /** 生产者确认级别 (0, 1, all) */
    @Value("${logging.kafka.producer.acks:1}")
    private String acks;

    /** 批量发送延迟时间（毫秒） */
    @Value("${logging.kafka.producer.linger-ms:100}")
    private int lingerMs;

    /** 批量大小（字节） */
    @Value("${logging.kafka.producer.batch-size:16384}")
    private int batchSize;

    /** 缓冲区大小（字节） */
    @Value("${logging.kafka.producer.buffer-memory:33554432}")
    private long bufferMemory;

    /** 压缩类型 (none, gzip, snappy, lz4, zstd) */
    @Value("${logging.kafka.producer.compression-type:snappy}")
    private String compressionType;

    /** 重试次数 */
    @Value("${logging.kafka.producer.retries:3}")
    private int retries;

    /** 最大阻塞时间（毫秒） */
    @Value("${logging.kafka.producer.max-block-ms:5000}")
    private long maxBlockMs;

    /** 异步队列大小 */
    @Value("${logging.kafka.async.queue-size:1024}")
    private int asyncQueueSize;

    /** 异步丢弃阈值 */
    @Value("${logging.kafka.async.discarding-threshold:0}")
    private int discardingThreshold;

    /** 是否包含调用者信息 */
    @Value("${logging.kafka.async.include-caller-data:false}")
    private boolean includeCallerData;

    /** 环境标识 */
    @Value("${spring.profiles.active:dev}")
    private String environment;

    /** 服务名称 */
    @Value("${spring.application.name:data-collector}")
    private String serviceName;

    /**
     * 配置初始化后验证
     */
    @PostConstruct
    public void validateConfig() {
        log.info("=== Kafka 日志配置初始化 ===");
        log.info("启用状态: {}", enabled);
        log.info("日志级别: {}", level);
        log.info("Kafka 主题: {}", topic);
        log.info("Kafka 集群: {}", bootstrapServers);
        log.info("环境标识: {}", environment);
        log.info("服务名称: {}", serviceName);
        
        if (enabled) {
            log.info("生产者配置 - acks: {}, linger: {}ms, batch: {}bytes, buffer: {}bytes", 
                    acks, lingerMs, batchSize, bufferMemory);
            log.info("异步配置 - queue: {}, threshold: {}, caller: {}", 
                    asyncQueueSize, discardingThreshold, includeCallerData);
            
            // 验证日志级别
            if (!isValidLogLevel(level)) {
                log.warn("无效的日志级别: {}, 将使用默认值 INFO", level);
                this.level = "INFO";
            }
            
            // 验证压缩类型
            if (!isValidCompressionType(compressionType)) {
                log.warn("无效的压缩类型: {}, 将使用默认值 snappy", compressionType);
                this.compressionType = "snappy";
            }
            
            log.info("Kafka 日志推送配置验证通过");
        } else {
            log.info("Kafka 日志推送已禁用");
        }
        log.info("=== Kafka 日志配置初始化完成 ===");
    }

    /**
     * 验证日志级别是否有效
     */
    private boolean isValidLogLevel(String level) {
        return level != null && level.matches("(?i)(TRACE|DEBUG|INFO|WARN|ERROR|OFF)");
    }

    /**
     * 验证压缩类型是否有效
     */
    private boolean isValidCompressionType(String compressionType) {
        return compressionType != null && 
               compressionType.matches("(?i)(none|gzip|snappy|lz4|zstd)");
    }

    /**
     * 获取完整的实例标识
     */
    public String getInstanceId() {
        return String.format("%s-%s", serviceName, environment);
    }

    /**
     * 判断是否应该推送指定级别的日志
     */
    public boolean shouldLog(String logLevel) {
        if (!enabled) {
            return false;
        }
        
        int configLevel = getLogLevelValue(this.level);
        int currentLevel = getLogLevelValue(logLevel);
        
        return currentLevel >= configLevel;
    }

    /**
     * 获取日志级别数值（用于比较）
     */
    private int getLogLevelValue(String level) {
        return switch (level.toUpperCase()) {
            case "TRACE" -> 0;
            case "DEBUG" -> 1;
            case "INFO" -> 2;
            case "WARN" -> 3;
            case "ERROR" -> 4;
            case "OFF" -> 5;
            default -> 2; // 默认 INFO
        };
    }
}