package com.hao.strategyengine.monitoring;

import org.springframework.stereotype.Component;

import java.lang.management.ClassLoadingMXBean;
import java.lang.management.GarbageCollectorMXBean;
import java.lang.management.ManagementFactory;
import java.lang.management.MemoryMXBean;
import java.lang.management.MemoryUsage;
import java.lang.management.RuntimeMXBean;
import java.time.Duration;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * JVM运行时监控,聚合堆内存、类加载、GC等核心指标。
 */
@Component
public class JvmRuntimeMonitor {

    private final MemoryMXBean memoryMXBean = ManagementFactory.getMemoryMXBean();
    private final RuntimeMXBean runtimeMXBean = ManagementFactory.getRuntimeMXBean();
    private final ClassLoadingMXBean classLoadingMXBean = ManagementFactory.getClassLoadingMXBean();

    /**
     * 获取堆/非堆内存使用详情。
     */
    public Map<String, Map<String, Object>> captureMemoryUsage() {
        Map<String, Map<String, Object>> usage = new HashMap<>();
        usage.put("heap", convert(memoryMXBean.getHeapMemoryUsage()));
        usage.put("nonHeap", convert(memoryMXBean.getNonHeapMemoryUsage()));
        return usage;
    }

    /**
     * 获取类加载相关指标。
     */
    public Map<String, Object> captureClassLoadingStats() {
        Map<String, Object> stats = new HashMap<>();
        stats.put("loadedClassCount", classLoadingMXBean.getLoadedClassCount());
        stats.put("totalLoadedClassCount", classLoadingMXBean.getTotalLoadedClassCount());
        stats.put("unloadedClassCount", classLoadingMXBean.getUnloadedClassCount());
        return stats;
    }

    /**
     * 返回JVM启动时长、输入参数等基础信息。
     */
    public Map<String, Object> captureRuntimeInfo() {
        Map<String, Object> runtimeInfo = new HashMap<>();
        runtimeInfo.put("vmName", runtimeMXBean.getVmName());
        runtimeInfo.put("vmVendor", runtimeMXBean.getVmVendor());
        runtimeInfo.put("vmVersion", runtimeMXBean.getVmVersion());
        long uptimeMillis = runtimeMXBean.getUptime();
        runtimeInfo.put("uptime", Duration.ofMillis(uptimeMillis));
        runtimeInfo.put("uptimeMillis", uptimeMillis);
        runtimeInfo.put("startTime", runtimeMXBean.getStartTime());
        runtimeInfo.put("inputArguments", runtimeMXBean.getInputArguments());
        return runtimeInfo;
    }

    /**
     * 汇总所有GC的统计信息。
     */
    public List<Map<String, Object>> captureGarbageCollectorStats() {
        return ManagementFactory.getGarbageCollectorMXBeans().stream()
                .map(this::convert)
                .collect(Collectors.toList());
    }

    private Map<String, Object> convert(MemoryUsage usage) {
        Map<String, Object> result = new HashMap<>();
        result.put("init", usage.getInit());
        result.put("used", usage.getUsed());
        result.put("committed", usage.getCommitted());
        result.put("max", usage.getMax());
        return result;
    }

    private Map<String, Object> convert(GarbageCollectorMXBean gcBean) {
        Map<String, Object> result = new HashMap<>();
        result.put("name", gcBean.getName());
        result.put("collectionCount", gcBean.getCollectionCount());
        result.put("collectionTime", gcBean.getCollectionTime());
        result.put("memoryPoolNames", gcBean.getMemoryPoolNames());
        return result;
    }
}
