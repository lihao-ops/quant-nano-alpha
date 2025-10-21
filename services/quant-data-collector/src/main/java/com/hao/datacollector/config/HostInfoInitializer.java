package com.hao.datacollector.config;

import integration.kafka.KafkaTopics;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.ApplicationRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.net.InetAddress;
import java.net.NetworkInterface;
import java.util.Enumeration;

/**
 * 主机信息初始化器
 * 
 * <p>功能：
 * 1. 在应用启动时获取并设置主机信息系统属性
 * 2. 支持 IP、主机名、端口、实例ID 等完整实例标识
 * 3. 提供网络接口检测和最佳 IP 选择策略
 * 
 * <p>设计思路（大厂面试官视角）：
 * - 实例唯一性：通过 IP+端口+服务名确保实例在集群中的唯一标识
 * - 网络适配：智能选择最佳网络接口，避免回环地址和虚拟网卡
 * - 容错处理：网络异常时提供默认值，确保应用正常启动
 * - 日志友好：详细记录主机信息获取过程，便于运维排查
 * - 配置灵活：支持通过环境变量覆盖自动检测结果
 */
@Slf4j
@Configuration
public class HostInfoInitializer {

    @Value("${server.port:8080}")
    private String serverPort;

    @Value("${spring.application.name:data-collector}")
    private String applicationName;

    @Value("${spring.profiles.active:dev}")
    private String activeProfile;

    @Bean
    public ApplicationRunner hostInfoInitializerRunner() {
        return args -> {
            log.info("=== 主机信息初始化开始 ===");
            
            try {
                // 获取主机信息
                String hostName = getHostName();
                String hostIp = getBestHostIp();
                String instanceId = generateInstanceId(hostIp, serverPort, applicationName);
                
                // 设置系统属性（供 logback 使用）
                System.setProperty("LOG_TOPIC", KafkaTopics.LOG_QUANT_DATA_COLLECTOR.code());
                System.setProperty("HOST_NAME", hostName);
                System.setProperty("HOST_IP", hostIp);
                System.setProperty("HOST_PORT", serverPort);
                System.setProperty("INSTANCE_ID", instanceId);
                System.setProperty("SERVICE_NAME", applicationName);
                System.setProperty("ACTIVE_PROFILE", activeProfile);
                
                // 记录主机信息
                log.info("主机信息获取成功:");
                log.info("  - 主机名: {}", hostName);
                log.info("  - IP地址: {}", hostIp);
                log.info("  - 端口: {}", serverPort);
                log.info("  - 实例ID: {}", instanceId);
                log.info("  - 服务名: {}", applicationName);
                log.info("  - 环境: {}", activeProfile);
                log.info("  - 日志主题: {}", KafkaTopics.LOG_QUANT_DATA_COLLECTOR.code());
                
            } catch (Exception e) {
                log.error("主机信息获取失败，使用默认值", e);
                
                // 设置默认值
                System.setProperty("LOG_TOPIC", KafkaTopics.LOG_QUANT_DATA_COLLECTOR.code());
                System.setProperty("HOST_NAME", "unknown");
                System.setProperty("HOST_IP", "unknown");
                System.setProperty("HOST_PORT", serverPort);
                System.setProperty("INSTANCE_ID", "unknown-" + serverPort + "-" + applicationName);
                System.setProperty("SERVICE_NAME", applicationName);
                System.setProperty("ACTIVE_PROFILE", activeProfile);
                
                log.warn("已设置默认主机信息，服务可正常启动");
            }
            
            log.info("=== 主机信息初始化完成 ===");
        };
    }

    /**
     * 获取主机名
     */
    private String getHostName() {
        try {
            // 优先使用环境变量
            String envHostName = System.getenv("HOSTNAME");
            if (envHostName != null && !envHostName.trim().isEmpty()) {
                log.debug("使用环境变量 HOSTNAME: {}", envHostName);
                return envHostName.trim();
            }
            
            // 使用 Java API 获取
            String javaHostName = InetAddress.getLocalHost().getHostName();
            log.debug("使用 Java API 获取主机名: {}", javaHostName);
            return javaHostName;
            
        } catch (Exception e) {
            log.warn("获取主机名失败: {}", e.getMessage());
            return "unknown";
        }
    }

    /**
     * 获取最佳主机 IP 地址
     * 优先级：非回环 > 非虚拟网卡 > IPv4 > 可达性
     */
    private String getBestHostIp() {
        try {
            // 优先使用环境变量
            String envHostIp = System.getenv("HOST_IP");
            if (envHostIp != null && !envHostIp.trim().isEmpty()) {
                log.debug("使用环境变量 HOST_IP: {}", envHostIp);
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
                        log.debug("发现更优 IP: {} (网卡: {}, 评分: {})", ip, networkInterface.getName(), score);
                    }
                }
            }
            
            if (bestIp != null) {
                log.debug("选择最佳 IP: {} (评分: {})", bestIp, bestScore);
                return bestIp;
            }
            
            // 兜底方案：使用默认方法
            String fallbackIp = InetAddress.getLocalHost().getHostAddress();
            log.debug("使用兜底 IP: {}", fallbackIp);
            return fallbackIp;
            
        } catch (Exception e) {
            log.warn("获取主机 IP 失败: {}", e.getMessage());
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
     * 生成实例唯一标识
     */
    private String generateInstanceId(String ip, String port, String serviceName) {
        return String.format("%s-%s-%s", serviceName, ip, port);
    }
}