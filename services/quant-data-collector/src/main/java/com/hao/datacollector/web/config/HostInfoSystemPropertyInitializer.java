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
 * 设计目的：
 * 1. 在logback初始化前设置主机信息系统属性。
 * 2. 确保日志系统能够读取到主机名与IP。
 *
 * 为什么需要该类：
 * - 需要在日志系统初始化前完成主机信息注入。
 *
 * 核心实现思路：
 * - 启动时读取环境变量与网卡信息并写入系统属性。
 *
 * <p>执行时机：使用@Order(Ordered.HIGHEST_PRECEDENCE)确保最早执行
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
        // 实现思路：
        // 1. 获取主机名与IP并写入系统属性。
        // 2. 异常时设置默认值兜底。
        log.info("主机信息系统属性初始化开始|Host_info_sysprop_init_start");

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
            
            log.info("主机信息系统属性设置完成|Host_info_sysprop_set_done");
            log.info("日志记录|Log_message,HOST_NAME|Host_name,name={}", hostName);
            log.info("日志记录|Log_message,HOST_IP|Host_ip,ip={}", hostIp);
            log.info("日志记录|Log_message,server.port|Server_port,port={}", serverPort);
            log.info("日志记录|Log_message,spring.application.name|Service_name,name={}", serviceName);
            log.info("日志记录|Log_message,spring.profiles.active|Active_profile,profile={}", env);
            log.info("日志记录|Log_message,logging.kafka.topic|Log_topic,topic=log-{}", serviceName);
            
        } catch (Exception e) {
            log.error("主机信息系统属性设置失败|Host_info_sysprop_set_failed,error={}", e.getMessage(), e);
            
            // 设置默认值
            System.setProperty("HOST_NAME", "unknown");
            System.setProperty("HOST_IP", "unknown");
            System.setProperty("server.port", serverPort);
            System.setProperty("spring.application.name", serviceName);
            System.setProperty("spring.profiles.active", env);
            System.setProperty("logging.kafka.topic", "log-" + serviceName);
            
            log.warn("已设置默认主机信息系统属性|Default_sysprop_applied");
        }
    }

    /**
     * 获取主机名
     *
     * 实现逻辑：
     * 1. 优先读取环境变量HOSTNAME。
     * 2. 回退到JavaAPI获取。
     *
     * @return 主机名
     */
    private String getHostName() {
        // 实现思路：
        // 1. 优先使用环境变量。
        // 2. 失败时回退到JavaAPI。
        try {
            // 优先使用环境变量
            String envHostName = System.getenv("HOSTNAME");
            if (envHostName != null && !envHostName.trim().isEmpty()) {
                return envHostName.trim();
            }
            
            // 使用 Java API 获取
            return InetAddress.getLocalHost().getHostName();
            
        } catch (Exception e) {
            log.debug("获取主机名失败|Host_name_load_failed,error={}", e.getMessage());
            return "unknown";
        }
    }

    /**
     * 获取最佳主机 IP 地址
     *
     * 实现逻辑：
     * 1. 读取环境变量HOST_IP优先返回。
     * 2. 遍历网卡选择最佳IP。
     *
     * @return 主机IP
     */
    private String getBestHostIp() {
        // 实现思路：
        // 1. 优先使用环境变量IP。
        // 2. 根据评分选择最佳网卡IP。
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
            log.debug("获取主机IP失败|Host_ip_load_failed,error={}", e.getMessage());
            return "unknown";
        }
    }

    /**
     * 计算 IP 地址评分（分数越高越优）
     *
     * 实现逻辑：
     * 1. IPv4优先加分。
     * 2. 私网段与物理网卡优先加分。
     *
     * @param ip IP地址
     * @param interfaceName 网卡名称
     * @return 评分
     */
    private int calculateIpScore(String ip, String interfaceName) {
        // 实现思路：
        // 1. 以IP类型与网卡类型进行打分。
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
