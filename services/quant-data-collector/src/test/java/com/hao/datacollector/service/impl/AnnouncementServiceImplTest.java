package com.hao.datacollector.service.impl;

import com.hao.datacollector.common.cache.StockCache;
import com.hao.datacollector.common.constant.DateTimeFormatConstants;
import com.hao.datacollector.common.utils.DateUtil;
import com.hao.datacollector.dal.dao.AnnouncementMapper;
import com.hao.datacollector.dal.dao.BaseDataMapper;
import com.hao.datacollector.service.AnnouncementService;
import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CountDownLatch;

@Slf4j
@SpringBootTest
class AnnouncementServiceImplTest {

    @Autowired
    private AnnouncementService announcementService;

    @Autowired
    private AnnouncementMapper announcementMapper;

    @Autowired
    private BaseDataMapper baseDataMapper;

    /**
     * 注入在 ThreadPoolConfig 中定义的虚拟线程执行器。
     * 虚拟线程特别适合于高并发、IO密集型（如HTTP请求）的任务，因为它能以极低的资源开销处理大量阻塞操作。
     */
    @Resource(name = "virtualThreadExecutor")
    private java.util.concurrent.Executor virtualThreadExecutor;

    /**
     * 注入在 ThreadPoolConfig 中定义的IO密集型线程池。
     * IO密集型线程池特别适合于处理IO操作（如数据库查询、文件读取、网络请求）的场景。
     */
    @Resource(name = "ioTaskExecutor")
    private java.util.concurrent.Executor ioTaskExecutor;

    /**
     * 转档公告数据。
     * <p>
     * 该方法会遍历所有待处理的股票代码，并逐个调用 transferAnnouncement 方法进行公告转档。
     * </p>
     */
    @Test
    void transferAnnouncement() {
        //去除近期已转档过的代码
        List<String> jobStockList = StockCache.allWindCode;
//        String startDate = announcementMapper.getJobAnnouncementEndLastDate();
        String startDate = "20250704";
        String endDate = DateUtil.stringTimeToAdjust(DateUtil.getCurrentDateTimeByStr(DateTimeFormatConstants.EIGHT_DIGIT_DATE_FORMAT), DateTimeFormatConstants.EIGHT_DIGIT_DATE_FORMAT, 1);
        List<String> jobEndList = announcementMapper.getJobAnnouncementEndWindCodeList(startDate, endDate);
        jobStockList.removeAll(jobEndList);
        //删除异常股票列表
        List<String> abnormalStockList = baseDataMapper.getAbnormalStockList();
        jobStockList.removeAll(abnormalStockList);
        for (String windCode : jobStockList) {
            try {
                Boolean transferAnnouncementResult = announcementService.transferAnnouncement(windCode, startDate, endDate, 1, 500);
                log.info("AnnouncementServiceImplTest_transferAnnouncement_windCode={},transferAnnouncementResult={}", windCode, transferAnnouncementResult);
            } catch (Exception e) {
                log.error("AnnouncementServiceImplTest_transferAnnouncement_windCode={},e={}", windCode, e);
            }
        }
    }

    /**
     * 批量并发转储公告数据。(IO线程池调用)
     * <p>
     * 该方法利用IO密集型线程池并发处理所有待处理的股票代码，
     * 每个股票代码的公告转储操作作为一个独立的异步任务提交。
     * 使用 CountDownLatch 来等待所有任务执行完成。
     * </p>
     */
    @Test
    void transferAnnouncementIOBatch() {
        //去除近期已转档过的代码
        // 通过创建副本确保线程安全，避免直接修改共享的静态缓存 StockCache.allWindCode
        List<String> jobStockList = new java.util.ArrayList<>(StockCache.allWindCode);
        String startDate = announcementMapper.getJobAnnouncementEndLastDate();
        String endDate = DateUtil.stringTimeToAdjust(DateUtil.getCurrentDateTimeByStr(DateTimeFormatConstants.EIGHT_DIGIT_DATE_FORMAT), DateTimeFormatConstants.EIGHT_DIGIT_DATE_FORMAT, 1);
        List<String> jobEndList = announcementMapper.getJobAnnouncementEndWindCodeList(startDate, endDate);
        jobStockList.removeAll(jobEndList);
        //删除异常股票列表
        List<String> abnormalStockList = baseDataMapper.getAbnormalStockList();
        jobStockList.removeAll(abnormalStockList);

        CountDownLatch latch = new CountDownLatch(jobStockList.size());
        for (String windCode : jobStockList) {
            CompletableFuture.runAsync(() -> {
                try {
                    Boolean transferAnnouncementResult = announcementService.transferAnnouncement(windCode, startDate, endDate, 1, 500);
                    log.info("AnnouncementServiceImplTest_transferAnnouncement_windCode={},transferAnnouncementResult={}", windCode, transferAnnouncementResult);
                } catch (Exception e) {
                    log.error("AnnouncementServiceImplTest_transferAnnouncement_windCode={},e={}", windCode, e);
                } finally {
                    latch.countDown();
                }
            }, ioTaskExecutor);
        }
        try {
            latch.await();
        } catch (InterruptedException e) {
            log.error("transferAnnouncementBatch interrupted", e);
        }
    }


    /**
     * 批量并发转储公告数据。(虚拟线程池调用)
     * <p>
     * 该方法利用IO密集型线程池并发处理所有待处理的股票代码，
     * 每个股票代码的公告转储操作作为一个独立的异步任务提交。
     * 使用 CountDownLatch 来等待所有任务执行完成。
     * </p>
     */
    @Test
    void transferAnnouncementVirtualBatch() {
        //去除近期已转档过的代码
        // 通过创建副本确保线程安全，避免直接修改共享的静态缓存 StockCache.allWindCode
        List<String> jobStockList = new java.util.ArrayList<>(StockCache.allWindCode);
        String startDate = announcementMapper.getJobAnnouncementEndLastDate();
        String endDate = DateUtil.stringTimeToAdjust(DateUtil.getCurrentDateTimeByStr(DateTimeFormatConstants.EIGHT_DIGIT_DATE_FORMAT), DateTimeFormatConstants.EIGHT_DIGIT_DATE_FORMAT, 1);
        List<String> jobEndList = announcementMapper.getJobAnnouncementEndWindCodeList(startDate, endDate);
        jobStockList.removeAll(jobEndList);
        //删除异常股票列表
        List<String> abnormalStockList = baseDataMapper.getAbnormalStockList();
        jobStockList.removeAll(abnormalStockList);

        CountDownLatch latch = new CountDownLatch(jobStockList.size());
        for (String windCode : jobStockList) {
            CompletableFuture.runAsync(() -> {
                try {
                    Boolean transferAnnouncementResult = announcementService.transferAnnouncement(windCode, startDate, endDate, 1, 500);
                    log.info("AnnouncementServiceImplTest_transferAnnouncement_windCode={},transferAnnouncementResult={}", windCode, transferAnnouncementResult);
                } catch (Exception e) {
                    log.error("AnnouncementServiceImplTest_transferAnnouncement_windCode={},e={}", windCode, e);
                } finally {
                    latch.countDown();
                }
            }, virtualThreadExecutor);
        }
        try {
            latch.await();
        } catch (InterruptedException e) {
            log.error("transferAnnouncementBatch interrupted", e);
        }
    }

    @Test
    void transferEvent() {
        //去除近期已转档过的代码
        List<String> jobStockList = StockCache.allWindCode;
//        String startDate = announcementMapper.getJobEventEndLastDate();
        String startDate = "20250612";
        String endDate = DateUtil.stringTimeToAdjust(DateUtil.getCurrentDateTimeByStr(DateTimeFormatConstants.EIGHT_DIGIT_DATE_FORMAT), DateTimeFormatConstants.EIGHT_DIGIT_DATE_FORMAT, 1);
        List<String> jobEndList = announcementMapper.getJobEventEndWindCodeList(startDate, endDate);
        jobStockList.removeAll(jobEndList);
        //删除异常股票列表
        List<String> abnormalStockList = baseDataMapper.getAbnormalStockList();
        jobStockList.removeAll(abnormalStockList);
        for (String windCode : jobStockList) {
            Boolean transferEventResult = announcementService.transferEvent(windCode, startDate, endDate, 1, 500);
            log.info("AnnouncementServiceImplTest_transferEvent_windCode={},transferEventResult={}", windCode, transferEventResult);
        }
    }

    /**
     * 批量并发转储事件数据。
     * <p>
     * 该方法利用IO密集型线程池并发处理所有待处理的股票代码，
     * 每个股票代码的事件转储操作作为一个独立的异步任务提交。
     * 使用 CountDownLatch 来等待所有任务执行完成。
     * </p>
     */
    @Test
    void transferEventBatch() {
        //去除近期已转档过的代码
        // 通过创建副本确保线程安全，避免直接修改共享的静态缓存 StockCache.allWindCode
        List<String> jobStockList = new java.util.ArrayList<>(StockCache.allWindCode);
        String startDate = announcementMapper.getJobEventEndLastDate();
        String endDate = DateUtil.stringTimeToAdjust(DateUtil.getCurrentDateTimeByStr(DateTimeFormatConstants.EIGHT_DIGIT_DATE_FORMAT), DateTimeFormatConstants.EIGHT_DIGIT_DATE_FORMAT, 1);
        List<String> jobEndList = announcementMapper.getJobEventEndWindCodeList(startDate, endDate);
        jobStockList.removeAll(jobEndList);
        //删除异常股票列表
        List<String> abnormalStockList = baseDataMapper.getAbnormalStockList();
        jobStockList.removeAll(abnormalStockList);

        CountDownLatch latch = new CountDownLatch(jobStockList.size());
        for (String windCode : jobStockList) {
            CompletableFuture.runAsync(() -> {
                try {
                    Boolean transferEventResult = announcementService.transferEvent(windCode, startDate, endDate, 1, 500);
                    log.info("AnnouncementServiceImplTest_transferEvent_windCode={},transferEventResult={}", windCode, transferEventResult);
                } catch (Exception e) {
                    log.error("AnnouncementServiceImplTest_transferEvent_windCode={},e={}", windCode, e);
                } finally {
                    latch.countDown();
                }
            }, virtualThreadExecutor);
        }
        try {
            latch.await();
        } catch (InterruptedException e) {
            log.error("transferEventBatch interrupted", e);
        }
    }
}