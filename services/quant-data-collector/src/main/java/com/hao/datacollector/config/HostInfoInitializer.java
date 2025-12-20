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
 * 设计目的：
 * 1. 启动时初始化主机信息并写入系统属性。
 * 2. 统一生成实例标识，便于日志与监控聚合。
 *
 * 为什么需要该类：
 * - 多实例部署需要统一的实例标识与主机信息采集。
 *
 * 核心实现思路：
 * - 先读取环境变量，失败则通过网络接口扫描获取最佳IP。
 * - 构建实例ID后写入系统属性供日志系统使用。
 *
 * <p>功能：
 * 1. 在应用启动时获取并设置主机信息系统属性
 * 2. 支持IP、主机名、端口、实例ID等完整实例标识
 * 3. 提供网络接口检测和最佳IP选择策略
 *
 * <p>设计思路（大厂面试官视角）：
 * - 实例唯一性：通过IP+端口+服务名确保实例在集群中的唯一标识
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

    /**
     * 主机信息初始化Runner
     *
     * 实现逻辑：
     * 1. 获取主机名与IP并生成实例ID。
     * 2. 写入系统属性并记录日志。
     * 3. 异常时设置默认值兜底。
     *
     * @return ApplicationRunner回调
     */
    @Bean
    public ApplicationRunner hostInfoInitializerRunner() {
        return args -> {
            // 实现思路：
            // 1. 获取主机信息并写入系统属性。
            // 2. 异常时使用默认值。
            log.info("主机信息初始化开始|Host_info_init_start");
            
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
                log.info("主机信息获取成功|Host_info_loaded");
                log.info("主机名|Host_name,name={}", hostName);
                log.info("主机IP|Host_ip,ip={}", hostIp);
                log.info("服务端口|Service_port,port={}", serverPort);
                log.info("实例ID|Instance_id,instanceId={}", instanceId);
                log.info("服务名|Service_name,name={}", applicationName);
                log.info("运行环境|Active_profile,profile={}", activeProfile);
                log.info("日志主题|Log_topic,topic={}", KafkaTopics.LOG_QUANT_DATA_COLLECTOR.code());
                
            } catch (Exception e) {
                log.error("主机信息获取失败|Host_info_load_failed", e);
                
                // 设置默认值
                System.setProperty("LOG_TOPIC", KafkaTopics.LOG_QUANT_DATA_COLLECTOR.code());
                System.setProperty("HOST_NAME", "unknown");
                System.setProperty("HOST_IP", "unknown");
                System.setProperty("HOST_PORT", serverPort);
                System.setProperty("INSTANCE_ID", "unknown-" + serverPort + "-" + applicationName);
                System.setProperty("SERVICE_NAME", applicationName);
                System.setProperty("ACTIVE_PROFILE", activeProfile);
                
                log.warn("已设置默认主机信息|Default_host_info_applied");
            }
            
            log.info("主机信息初始化完成|Host_info_init_done");
        };
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
                log.debug("使用环境变量HOSTNAME|Use_env_hostname,hostname={}", envHostName);
                return envHostName.trim();
            }
            
            // 使用 Java API 获取
            String javaHostName = InetAddress.getLocalHost().getHostName();
            log.debug("使用JavaAPI获取主机名|Use_java_api_hostname,hostname={}", javaHostName);
            return javaHostName;
            
        } catch (Exception e) {
            log.warn("获取主机名失败|Host_name_load_failed,error={}", e.getMessage(), e);
            return "unknown";
        }
    }

    /**
     * 获取最佳主机 IP 地址
     * 优先级：非回环 > 非虚拟网卡 > IPv4 > 可达性
     *
     * 实现逻辑：
     * 1. 读取环境变量HOST_IP优先返回。
     * 2. 遍历网卡并按评分选择最佳IP。
     *
     * @return 主机IP
     */
    private String getBestHostIp() {
        // 实现思路：
        // 1. 优先使用环境变量IP。
        // 2. 通过评分选择最佳网卡IP。
        try {
            // 优先使用环境变量
            String envHostIp = System.getenv("HOST_IP");
            if (envHostIp != null && !envHostIp.trim().isEmpty()) {
                log.debug("使用环境变量HOST_IP|Use_env_host_ip,ip={}", envHostIp);
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
                        log.debug("发现更优IP|Better_ip_found,ip={},interface={},score={}",
                                ip, networkInterface.getName(), score);
                    }
                }
            }
            
            if (bestIp != null) {
                log.debug("选择最佳IP|Best_ip_selected,ip={},score={}", bestIp, bestScore);
                return bestIp;
            }
            
            // 兜底方案：使用默认方法
            String fallbackIp = InetAddress.getLocalHost().getHostAddress();
            log.debug("使用兜底IP|Fallback_ip_used,ip={}", fallbackIp);
            return fallbackIp;
            
        } catch (Exception e) {
            log.warn("获取主机IP失败|Host_ip_load_failed,error={}", e.getMessage(), e);
            return "unknown";
        }
    }

    /**
     * 计算IP地址评分（分数越高越优）
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
        
        // IPv4优于IPv6
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
     *
     * 实现逻辑：
     * 1. 使用服务名、IP与端口拼接实例ID。
     *
     * @param ip 主机IP
     * @param port 端口
     * @param serviceName 服务名
     * @return 实例ID
     */
    private String generateInstanceId(String ip, String port, String serviceName) {
        // 实现思路：
        // 1. 拼接服务名、IP与端口。
        return String.format("%s-%s-%s", serviceName, ip, port);
    }
}
