package com.hao.datacollector.web.config;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;

import java.net.InetAddress;
import java.net.NetworkInterface;
import java.util.Enumeration;

/**
 * 主机信息系统属性初始化器
 * 
 * <p>目的：在 logback 初始化之前就设置好主机信息系统属性，
 * 确保 kafka-appender.xml 能够正确读取到 hostname 和 ip 信息。
 * 
 * <p>执行时机：使用 @Order(Ordered.HIGHEST_PRECEDENCE) 确保最早执行
 * 
 * @author quant-team
 * @since 2025-10-21
 */
@Slf4j
@Component
@Order(Ordered.HIGHEST_PRECEDENCE)
public class HostInfoSystemPropertyInitializer implements InitializingBean {

    @Value("${server.port:8080}")
    private String serverPort;

    @Value("${spring.application.name:data-collector}")
    private String serviceName;

    @Value("${spring.profiles.active:dev}")
    private String env;

    @Override
    public void afterPropertiesSet() throws Exception {
        log.info("=== 初始化主机信息系统属性 ===");
        
        try {
            // 获取主机信息
            String hostName = getHostName();
            String hostIp = getBestHostIp();
            
            // 设置系统属性，供 logback 使用
            System.setProperty("HOST_NAME", hostName);
            System.setProperty("HOST_IP", hostIp);
            System.setProperty("server.port", serverPort);
            System.setProperty("spring.application.name", serviceName);
            System.setProperty("spring.profiles.active", env);
            System.setProperty("logging.kafka.topic", "log-" + serviceName);
            
            log.info("主机信息系统属性设置完成:");
            log.info("  - HOST_NAME: {}", hostName);
            log.info("  - HOST_IP: {}", hostIp);
            log.info("  - server.port: {}", serverPort);
            log.info("  - spring.application.name: {}", serviceName);
            log.info("  - spring.profiles.active: {}", env);
            log.info("  - logging.kafka.topic: log-{}", serviceName);
            
        } catch (Exception e) {
            log.error("设置主机信息系统属性失败: {}", e.getMessage(), e);
            
            // 设置默认值
            System.setProperty("HOST_NAME", "unknown");
            System.setProperty("HOST_IP", "unknown");
            System.setProperty("server.port", serverPort);
            System.setProperty("spring.application.name", serviceName);
            System.setProperty("spring.profiles.active", env);
            System.setProperty("logging.kafka.topic", "log-" + serviceName);
            
            log.warn("已设置默认主机信息系统属性");
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
}