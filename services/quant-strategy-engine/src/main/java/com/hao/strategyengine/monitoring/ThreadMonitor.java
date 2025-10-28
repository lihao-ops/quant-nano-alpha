package com.hao.strategyengine.monitoring;

import org.springframework.stereotype.Component;

import java.lang.management.ManagementFactory;
import java.lang.management.ThreadInfo;
import java.lang.management.ThreadMXBean;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * 线程监控工具,用于统一输出线程运行时指标,帮助排查死锁、线程泄漏等问题。
 */
@Component
public class ThreadMonitor {

    private final ThreadMXBean threadMXBean = ManagementFactory.getThreadMXBean();

    /**
     * 获取线程运行时指标摘要。
     */
    public Map<String, Object> captureThreadSnapshot() {
        Map<String, Object> metrics = new HashMap<>();
        metrics.put("threadCount", threadMXBean.getThreadCount());
        metrics.put("peakThreadCount", threadMXBean.getPeakThreadCount());
        metrics.put("daemonThreadCount", threadMXBean.getDaemonThreadCount());
        metrics.put("totalStartedThreadCount", threadMXBean.getTotalStartedThreadCount());
        metrics.put("deadlockedThreadCount", countDeadlockedThreads());
        return metrics;
    }

    /**
     * 返回当前存在死锁的线程ID列表。
     */
    public long[] findDeadlockedThreadIds() {
        long[] deadlocked = threadMXBean.findDeadlockedThreads();
        return deadlocked == null ? new long[0] : deadlocked;
    }

    /**
     * 尝试开启线程CPU时间统计。
     */
    public void enableThreadCpuTimeIfSupported() {
        if (threadMXBean.isThreadCpuTimeSupported() && !threadMXBean.isThreadCpuTimeEnabled()) {
            threadMXBean.setThreadCpuTimeEnabled(true);
        }
    }

    /**
     * 获取指定线程的堆栈信息。
     */
    public Map<Long, String> getThreadStackTrace(long[] threadIds, int maxDepth) {
        ThreadInfo[] threadInfos = threadMXBean.getThreadInfo(threadIds, maxDepth);
        return Arrays.stream(threadInfos)
                .filter(info -> info != null)
                .collect(Collectors.toMap(ThreadInfo::getThreadId, this::formatStackTrace));
    }

    /**
     * 获取线程CPU时间(纳秒),需JVM支持线程CPU统计。
     */
    public Map<Long, Long> getThreadCpuTime(long[] threadIds) {
        if (!threadMXBean.isThreadCpuTimeSupported() || !threadMXBean.isThreadCpuTimeEnabled()) {
            return Collections.emptyMap();
        }
        Map<Long, Long> cpuTimes = new HashMap<>();
        for (long threadId : threadIds) {
            cpuTimes.put(threadId, threadMXBean.getThreadCpuTime(threadId));
        }
        return cpuTimes;
    }

    private long countDeadlockedThreads() {
        return findDeadlockedThreadIds().length;
    }

    private String formatStackTrace(ThreadInfo threadInfo) {
        StringBuilder builder = new StringBuilder();
        builder.append(threadInfo.getThreadName())
                .append(" (#")
                .append(threadInfo.getThreadId())
                .append(")\n");
        for (StackTraceElement element : threadInfo.getStackTrace()) {
            builder.append("    at ")
                    .append(element)
                    .append('\n');
        }
        return builder.toString();
    }
}
