package util;

import java.net.Inet4Address;
import java.net.InetAddress;
import java.net.NetworkInterface;
import java.util.ArrayList;
import java.util.Enumeration;
import java.util.List;
import java.util.stream.Collectors;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * @author: hli
 * @createTime: 2025/06/07
 * @description: IP工具类
 */
public class IPUtil {
    private static final Logger LOG = LoggerFactory.getLogger(IPUtil.class);
    /**
     * 保存本机所有IP地址
     */
    private static String IPS;

    /**
     * 获取IP后缀(后两段,冒号分隔)
     *
     * @return
     */
    public static String getIPString() {
        if (IPS == null) {
            IPS = getIP().stream().collect(Collectors.joining(","));
        }
        return IPS;
    }

    /**
     * 获取本机IP
     *
     * @return
     */
    public static List<String> getIP() {
        List<String> hostAddresses = new ArrayList<>();
        try {
            Enumeration<NetworkInterface> allNetInterfaces = NetworkInterface.getNetworkInterfaces();
            InetAddress ip;
            while (allNetInterfaces.hasMoreElements()) {
                NetworkInterface netInterface = allNetInterfaces.nextElement();
                if (netInterface.isLoopback() || netInterface.isVirtual() || !netInterface.isUp()) {
                    continue;
                } else {
                    Enumeration<InetAddress> addresses = netInterface.getInetAddresses();
                    while (addresses.hasMoreElements()) {
                        ip = addresses.nextElement();
                        if (ip != null && ip instanceof Inet4Address) {
                            hostAddresses.add(ip.getHostAddress());
                        }
                    }
                }
            }
        } catch (Exception e) {
            LOG.error("IP获取失败|Ip_fetch_failed,errorClass={}", e.getClass().getSimpleName(), e);
        }
        return hostAddresses;
    }
}
