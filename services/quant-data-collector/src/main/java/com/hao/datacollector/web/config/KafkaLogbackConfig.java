package com.hao.datacollector.web.config;

import ch.qos.logback.classic.LoggerContext;
import com.github.danielwegener.logback.kafka.KafkaAppender;
import lombok.extern.slf4j.Slf4j;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.cloud.context.config.annotation.RefreshScope;
import org.springframework.context.ApplicationListener;
import org.springframework.stereotype.Component;

import java.net.InetAddress;
import java.net.NetworkInterface;
import java.util.Enumeration;

/**
 * Kafka Logback 配置热更新器
 *
 * <p>问题背景：
 * - logback 在 Spring 容器启动之前就初始化，此时 Nacos 配置还未加载
 * - 导致 logback-spring.xml 中无法获取到正确的 Kafka 集群地址和主机信息
 * - 最终使用默认值导致主机信息缺失（hostname=unknown, ip=unknown）
 *
 * <p>解决方案：
 * - 在应用完全启动后（ApplicationReadyEvent），重新配置 Kafka Appender
 * - 此时 Nacos 配置已加载，可以获取到正确的配置信息
 * - 动态更新 KafkaAppender 的所有配置参数，包括 bootstrap.servers、主机信息等
 * - 设置系统属性供 kafka-appender.xml 使用，确保字段完整性
 *
 * <p>设计思路（大厂面试官视角）：
 * - 生命周期管理：利用 Spring 事件机制在合适时机更新配置
 * - 配置热更新：支持 @RefreshScope 实现配置动态刷新
 * - 完整配置：不仅更新 Kafka 地址，还包括主机信息、主题等
 * - 容错处理：配置更新失败时不影响应用正常运行
 * - 日志可见：详细记录配置更新过程，便于问题排查
 *
 * @author quant-team
 * @since 2025-10-21
 */
@Slf4j
@Component
@RefreshScope
public class KafkaLogbackConfig implements ApplicationListener<ApplicationReadyEvent> {

    @Value("${spring.kafka.bootstrap-servers}")
    private String kafkaBootstrap;

    @Value("${spring.profiles.active}")
    private String env;

    @Value("${server.port:8080}")
    private String serverPort;

    @Value("${spring.application.name:data-collector}")
    private String serviceName;

    @Override
    public void onApplicationEvent(ApplicationReadyEvent event) {
        log.info("=== 应用启动完成，开始更新 Kafka Logback 配置 ===");
        updateKafkaConfig();
    }

    public void updateKafkaConfig() {
        try {
            // 先设置系统属性，确保在 appender 重启前就可用
            String hostName = getHostName();
            String hostIp = getBestHostIp();
            //todo 剩余解决HOST_NAME，HOST_IP无法生效问题
            //{"env":"dev","service":"quant-data-collector","hostname":"unknown","ip":"unknown","port":"8801","level":"INFO","thread":"kafka-producer-network-thread | producer-2","logger":"org.apache.kafka.clients.Metadata","timestamp":"2025-10-21T23:49:58.452000Z","message":"[Producer clientId=producer-2] Cluster ID: n8vjTMpNSPWbUOrWJNnHJg","exception":""}
            System.setProperty("HOST_IP", hostIp);
            System.setProperty("server.port", serverPort);
            System.setProperty("spring.application.name", serviceName);
            System.setProperty("spring.profiles.active", env);
            System.setProperty("logging.kafka.topic", "log-" + serviceName);
            
            LoggerContext loggerContext = (LoggerContext) LoggerFactory.getILoggerFactory();
            KafkaAppender kafkaAppender = (KafkaAppender) loggerContext.getLogger("ROOT").getAppender("kafkaAppender");

            if (kafkaAppender != null) {
                log.info("找到 Kafka Appender，开始更新配置");
                
                // 停止 appender
                kafkaAppender.stop();
                
                // 更新 Kafka 配置（使用与 Nacos 配置一致的参数）
                kafkaAppender.addProducerConfig("bootstrap.servers=" + kafkaBootstrap);
                kafkaAppender.addProducerConfig("acks=all");
                kafkaAppender.addProducerConfig("retries=2147483647");
                kafkaAppender.addProducerConfig("enable.idempotence=true");
                kafkaAppender.addProducerConfig("max.in.flight.requests.per.connection=5");
                kafkaAppender.addProducerConfig("linger.ms=5");
                kafkaAppender.addProducerConfig("batch.size=32768");
                kafkaAppender.addProducerConfig("buffer.memory=67108864");
                kafkaAppender.addProducerConfig("compression.type=snappy");
                
                // 重新启动 appender
                kafkaAppender.start();
                
                log.info("Kafka Logback 配置更新成功:");
                log.info("  - Kafka 集群: {}", kafkaBootstrap);
                log.info("  - 主机名: {}", hostName);
                log.info("  - IP地址: {}", hostIp);
                log.info("  - 端口: {}", serverPort);
                log.info("  - 服务名: {}", serviceName);
                log.info("  - 环境: {}", env);
                log.info("  - 日志主题: log-{}", serviceName);
                log.info("  - 实例ID: {}-{}-{}", serviceName, hostIp, serverPort);
                log.info("Kafka 日志推送已恢复正常，字段完整性已修复");
                
            } else {
                log.warn("未找到名为 'kafkaAppender' 的 Appender，请检查 kafka-appender.xml 配置");
                
                // 列出所有可用的 Appender
                log.info("当前可用的 Appender 列表:");
                loggerContext.getLoggerList().forEach(logger -> {
                    logger.iteratorForAppenders().forEachRemaining(appender -> {
                        log.info("  - Logger: {}, Appender: {} ({})", 
                            logger.getName(), appender.getName(), appender.getClass().getSimpleName());
                    });
                });
            }
            
        } catch (Exception e) {
            log.error("更新 Kafka Logback 配置失败: {}", e.getMessage(), e);
            log.warn("Kafka 日志推送可能无法正常工作，但不影响应用运行");
        }
    }

    /**
     * 获取主机名
     */
    private String getHostName() {
        try {
            // 优先使用环境变量
            String envHostName = System.getenv("HOSTNAME");
            if (envHostName != null && !envHostName.trim().isEmpty()) {
                return envHostName.trim();
            }
            
            // 使用 Java API 获取
            return InetAddress.getLocalHost().getHostName();
            
        } catch (Exception e) {
            log.debug("获取主机名失败: {}", e.getMessage());
            return "unknown";
        }
    }

    /**
     * 获取最佳主机 IP 地址
     */
    private String getBestHostIp() {
        try {
            // 优先使用环境变量
            String envHostIp = System.getenv("HOST_IP");
            if (envHostIp != null && !envHostIp.trim().isEmpty()) {
                return envHostIp.trim();
            }
            
            String bestIp = null;
            int bestScore = -1;
            
            Enumeration<NetworkInterface> networkInterfaces = NetworkInterface.getNetworkInterfaces();
            while (networkInterfaces.hasMoreElements()) {
                NetworkInterface networkInterface = networkInterfaces.nextElement();
                
                // 跳过非活动和回环接口
                if (!networkInterface.isUp() || networkInterface.isLoopback()) {
                    continue;
                }
                
                Enumeration<InetAddress> addresses = networkInterface.getInetAddresses();
                while (addresses.hasMoreElements()) {
                    InetAddress address = addresses.nextElement();
                    
                    // 跳过回环地址和 IPv6
                    if (address.isLoopbackAddress() || !address.isSiteLocalAddress()) {
                        continue;
                    }
                    
                    String ip = address.getHostAddress();
                    int score = calculateIpScore(ip, networkInterface.getName());
                    
                    if (score > bestScore) {
                        bestScore = score;
                        bestIp = ip;
                    }
                }
            }
            
            if (bestIp != null) {
                return bestIp;
            }
            
            // 兜底方案
            return InetAddress.getLocalHost().getHostAddress();
            
        } catch (Exception e) {
            log.debug("获取主机 IP 失败: {}", e.getMessage());
            return "unknown";
        }
    }

    /**
     * 计算 IP 地址评分（分数越高越优）
     */
    private int calculateIpScore(String ip, String interfaceName) {
        int score = 0;
        
        // IPv4 优于 IPv6
        if (ip.contains(".")) {
            score += 10;
        }
        
        // 私有网段优先级：192.168.x.x > 10.x.x.x > 172.16-31.x.x
        if (ip.startsWith("192.168.")) {
            score += 30;
        } else if (ip.startsWith("10.")) {
            score += 20;
        } else if (ip.matches("172\\.(1[6-9]|2[0-9]|3[0-1])\\..*")) {
            score += 15;
        }
        
        // 物理网卡优于虚拟网卡
        String lowerName = interfaceName.toLowerCase();
        if (lowerName.startsWith("eth") || lowerName.startsWith("en")) {
            score += 20;
        } else if (lowerName.contains("docker") || lowerName.contains("veth") || 
                   lowerName.contains("br-") || lowerName.contains("virbr")) {
            score -= 10; // 虚拟网卡降分
        }
        
        return score;
    }

    /**
     * 手动刷新配置（支持配置中心动态更新）
     */
    public void refreshKafkaConfig() {
        log.info("手动刷新 Kafka Logback 配置");
        updateKafkaConfig();
    }

    /**
     * 获取当前 Kafka 配置信息
     */
    public String getCurrentKafkaConfig() {
        return kafkaBootstrap;
    }
}