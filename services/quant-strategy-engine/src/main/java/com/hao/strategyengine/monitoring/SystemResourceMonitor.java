package com.hao.strategyengine.monitoring;

import org.springframework.stereotype.Component;

import java.io.File;
import java.lang.management.ManagementFactory;
import java.lang.management.OperatingSystemMXBean;
import java.util.HashMap;
import java.util.Map;

/**
 * 系统资源监控工具
 *
 * 用于采集CPU、内存、磁盘等基础系统指标,为上层业务报警/可视化提供统一数据来源。
 */
@Component
public class SystemResourceMonitor {

    private final OperatingSystemMXBean operatingSystemMXBean = ManagementFactory.getOperatingSystemMXBean();

    /**
     * 捕获一次系统资源快照(包含CPU、系统负载)。
     */
    public Map<String, Object> captureSystemLoadSnapshot() {
        Map<String, Object> snapshot = new HashMap<>();
        snapshot.put("availableProcessors", operatingSystemMXBean.getAvailableProcessors());
        snapshot.put("systemLoadAverage", operatingSystemMXBean.getSystemLoadAverage());
        snapshot.put("architecture", operatingSystemMXBean.getArch());
        snapshot.put("osName", operatingSystemMXBean.getName());
        snapshot.put("osVersion", operatingSystemMXBean.getVersion());
        return snapshot;
    }

    /**
     * 聚合一次系统资源的总体视图,方便直接输出或序列化到监控系统。
     */
    public Map<String, Object> captureResourceSummary(String diskPath) {
        Map<String, Object> summary = new HashMap<>();
        summary.put("load", captureSystemLoadSnapshot());
        summary.put("memory", captureMemoryUsage());
        summary.put("disk", captureDiskUsage(diskPath));
        return summary;
    }

    public Map<String, Object> captureResourceSummary() {
        return captureResourceSummary(null);
    }

    /**
     * 获取JVM层面的内存使用情况。
     */
    public Map<String, Long> captureMemoryUsage() {
        Runtime runtime = Runtime.getRuntime();
        Map<String, Long> memoryUsage = new HashMap<>();
        memoryUsage.put("total", runtime.totalMemory());
        memoryUsage.put("free", runtime.freeMemory());
        memoryUsage.put("used", runtime.totalMemory() - runtime.freeMemory());
        memoryUsage.put("max", runtime.maxMemory());
        return memoryUsage;
    }

    /**
     * 获取指定路径的磁盘使用情况,单位字节。
     *
     * @param path 待检查的路径,传 null 或空串时默认统计根路径
     */
    public Map<String, Object> captureDiskUsage(String path) {
        File file = (path == null || path.isBlank()) ? new File("/") : new File(path);
        Map<String, Object> diskUsage = new HashMap<>();
        diskUsage.put("path", file.getAbsolutePath());
        diskUsage.put("exists", file.exists());
        diskUsage.put("total", file.getTotalSpace());
        diskUsage.put("free", file.getFreeSpace());
        diskUsage.put("usable", file.getUsableSpace());
        diskUsage.put("used", file.getTotalSpace() - file.getFreeSpace());
        return diskUsage;
    }

    /**
     * 判断磁盘剩余空间是否低于阈值(单位字节)。
     */
    public boolean isDiskSpaceLow(String path, long thresholdBytes) {
        File file = (path == null || path.isBlank()) ? new File("/") : new File(path);
        return file.getUsableSpace() < thresholdBytes;
    }
}
