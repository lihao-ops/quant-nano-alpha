package com.hao.datacollector.web.config;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;
import org.springframework.stereotype.Component;

import java.util.concurrent.ThreadPoolExecutor;

/**
 * 线程池监控
 */
@Slf4j
@Component
public class ThreadPoolMonitor {
    
    @Autowired
    @Qualifier("ioTaskExecutor")
    private ThreadPoolTaskExecutor ioExecutor;
    
    @Autowired
    @Qualifier("cpuTaskExecutor") 
    private ThreadPoolTaskExecutor cpuExecutor;
    
    @Scheduled(fixedRate = 30000)
    public void monitorThreadPools() {
        logThreadPoolStats("IO", ioExecutor);
        logThreadPoolStats("CPU", cpuExecutor);
    }
    
    private void logThreadPoolStats(String type, ThreadPoolTaskExecutor executor) {
        ThreadPoolExecutor pool = executor.getThreadPoolExecutor();
        log.info("{} ThreadPool - Active: {}, Pool: {}, Queue: {}, Completed: {}", 
                type,
                pool.getActiveCount(),
                pool.getPoolSize(), 
                pool.getQueue().size(),
                pool.getCompletedTaskCount());
    }
}