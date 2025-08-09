package com.hao.datacollector.web.config;

import lombok.extern.slf4j.Slf4j;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CountDownLatch;

@Slf4j
@SpringBootTest
class ThreadPoolConfigTest {

    @Test
    void taskDataBaseLocalExecutor() {
    }

    @Autowired(required = false)
    @Qualifier("taskExecutor")
    private ThreadPoolTaskExecutor taskThreadPools;

    @Test
    void processByBatch() throws InterruptedException {
        //key=线程数,value=耗时s
        Map<Integer, Double> timeMap = new HashMap<Integer, Double>();

        for (int f = 4; f <= 50; f++) {
            //CPU核数为12,最好是不超过2N+1
            int threadCount = f; // 或者18、20
            Thread.sleep(1000); // 让线程池稳定
            long startTime = System.currentTimeMillis(); // 记录开始时间
            // 模拟5000+条数据
            List<String> dataList = new ArrayList<>();
            for (int j = 0; j < 5200; j++) {
                dataList.add("data-" + j);
            }
            int batchSize = (dataList.size() + threadCount - 1) / threadCount; // 1300
            CountDownLatch latch = new CountDownLatch(threadCount);
            log.info("总数据量：{}，线程数：{}，每线程处理：{}条", dataList.size(), threadCount, batchSize);
            for (int i = 0; i < threadCount; i++) {
                final int startIndex = i * batchSize;
                final int endIndex = Math.min(startIndex + batchSize, dataList.size());
                final int threadId = i;

                if (startIndex < dataList.size()) {
                    taskThreadPools.execute(() -> {
                        try {
                            log.info("线程{}开始处理，范围：{}-{}", Thread.currentThread().getName(), startIndex, endIndex - 1);
                            for (int j = startIndex; j < endIndex; j++) {
                                processData(dataList.get(j), threadId);
                            }
                            log.info("线程{}完成处理", Thread.currentThread().getName());
                        } finally {
                            latch.countDown();
                        }
                    });
                }
            }
            latch.await();

            long endTime = System.currentTimeMillis(); // 记录结束时间
            long totalTime = endTime - startTime; // 计算总耗时
            log.info("所有数据处理完成，总耗时：{}ms，约{}秒", totalTime, totalTime / 1000.0);
            timeMap.put(threadCount, totalTime / 1000.0);
        }
        for (Map.Entry<Integer, Double> entry : timeMap.entrySet()) {
            log.error("线程数={},执行耗时={}s", entry.getKey(), entry.getValue());
        }
    }

    private void processData(String data, int threadId) {
        try {
            Thread.sleep(50);
            log.info("ThreadId={}, ThreadName={}, 处理数据: {}",
                    threadId, Thread.currentThread().getName(), data);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
    }
}