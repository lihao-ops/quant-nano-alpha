package com.hao.datacollector.service;

import com.hao.datacollector.cache.StockCache;
import com.hao.datacollector.dal.dao.SimpleF9Mapper;
import com.hao.datacollector.dto.param.f9.F9Param;
import lombok.extern.slf4j.Slf4j;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import java.util.ArrayList;
import java.util.List;

/**
 * F9接口数据采集测试类
 * <p>
 * 测试目的：
 * 1. 验证F9接口数据获取流程是否正常。
 * 2. 验证批量转档逻辑在真实数据下的稳定性。
 * <p>
 * 设计思路：
 * - 基于Spring Boot容器注入服务与Mapper。
 * - 使用每个表的最大转档日期作为去重基准，保证断点续传的数据完整性。
 * - 遇到403 Forbidden（session超限）立即中断。
 */
@Slf4j
@SpringBootTest
public class SimpleF9ServiceTest {
    @Autowired
    private SimpleF9Service simpleF9Service;

    @Autowired
    private SimpleF9Mapper simpleF9Mapper;

    /**
     * 批量转档公司概览数据
     * <p>
     * 以表中最大转档日期为基准，剔除该日期已完成的windCode，转档剩余部分。
     */
    @Test
    void insertCompanyProfileDataJob() {
        String maxDate = simpleF9Mapper.getMaxCompanyProfileUpdateDate();
        log.info("公司简介最大转档日期={}", maxDate);
        if (maxDate == null) {
            maxDate = "1970-01-01";
        }
        List<String> allWindCode = new ArrayList<>(StockCache.allWindCode);
        List<String> endWindCodeList = simpleF9Mapper.getInsertFinancialSummaryData(maxDate);
        allWindCode.removeAll(endWindCodeList);
        log.info("公司简介待转档数量={}", allWindCode.size());
        for (String windCode : allWindCode) {
            F9Param f9Param = new F9Param();
            f9Param.setWindCode(windCode);
            try {
                Boolean result = simpleF9Service.insertCompanyProfileDataJob(f9Param);
                log.info("日志记录|Log_message,insertCompanyProfileDataJob.windCode={},result={}", windCode, result);
            } catch (Exception e) {
                if (e.getMessage().contains("403 Forbidden")) {
                    throw new RuntimeException(e.getMessage());
                }
                log.error("日志记录|Log_message,insertCompanyProfileDataJob.windCode={},error={}", windCode, e.getMessage(), e);
            }
        }
    }

    // ==================== 批量转档测试方法 ====================

    /**
     * 批量转档资讯信息
     */
    @Test
    void insertInformationDataJob() {
        String maxDate = simpleF9Mapper.getMaxInformationUpdateDate();
        log.info("资讯信息最大转档日期={}", maxDate);
        if (maxDate == null) {
            maxDate = "1970-01-01";
        }
        List<String> allWindCode = new ArrayList<>(StockCache.allWindCode);
        List<String> endWindCodeList = simpleF9Mapper.getInsertedInformationWindCodes(maxDate);
        allWindCode.removeAll(endWindCodeList);
        log.info("资讯信息待转档数量={}", allWindCode.size());
        for (String windCode : allWindCode) {
            F9Param f9Param = new F9Param();
            f9Param.setWindCode(windCode);
            try {
                Boolean result = simpleF9Service.insertInformationDataJob(f9Param);
                log.info("日志记录|Log_message,insertInformationDataJob.windCode={},result={}", windCode, result);
            } catch (Exception e) {
                if (e.getMessage().contains("403 Forbidden")) {
                    throw new RuntimeException(e.getMessage());
                }
                log.error("日志记录|Log_message,insertInformationDataJob.windCode={},error={}", windCode, e.getMessage(), e);
            }
        }
    }

    /**
     * 批量转档关键统计数据
     */
    @Test
    void insertKeyStatisticsDataJob() {
        String maxDate = simpleF9Mapper.getMaxKeyStatisticsUpdateDate();
        log.info("关键统计最大转档日期={}", maxDate);
        if (maxDate == null) {
            maxDate = "1970-01-01";
        }
        List<String> allWindCode = new ArrayList<>(StockCache.allWindCode);
        List<String> endWindCodeList = simpleF9Mapper.getInsertedKeyStatisticsWindCodes(maxDate);
        allWindCode.removeAll(endWindCodeList);
        log.info("关键统计待转档数量={}", allWindCode.size());
        for (String windCode : allWindCode) {
            F9Param f9Param = new F9Param();
            f9Param.setWindCode(windCode);
            try {
                Boolean result = simpleF9Service.insertKeyStatisticsDataJob(f9Param);
                log.info("日志记录|Log_message,insertKeyStatisticsDataJob.windCode={},result={}", windCode, result);
            } catch (Exception e) {
                if (e.getMessage().contains("403 Forbidden")) {
                    throw new RuntimeException(e.getMessage());
                }
                log.error("日志记录|Log_message,insertKeyStatisticsDataJob.windCode={},error={}", windCode, e.getMessage(), e);
            }
        }
    }

    /**
     * 批量转档公司信息
     */
    @Test
    void insertCompanyInfoDataJob() {
        String maxDate = simpleF9Mapper.getMaxCompanyInfoUpdateDate();
        log.info("公司信息最大转档日期={}", maxDate);
        if (maxDate == null) {
            maxDate = "1970-01-01";
        }
        List<String> allWindCode = new ArrayList<>(StockCache.allWindCode);
        List<String> endWindCodeList = simpleF9Mapper.getInsertedCompanyInfoWindCodes(maxDate);
        allWindCode.removeAll(endWindCodeList);
        log.info("公司信息待转档数量={}", allWindCode.size());
        for (String windCode : allWindCode) {
            F9Param f9Param = new F9Param();
            f9Param.setWindCode(windCode);
            try {
                Boolean result = simpleF9Service.insertCompanyInfoDataJob(f9Param);
                log.info("日志记录|Log_message,insertCompanyInfoDataJob.windCode={},result={}", windCode, result);
            } catch (Exception e) {
                if (e.getMessage().contains("403 Forbidden")) {
                    throw new RuntimeException(e.getMessage());
                }
                log.error("日志记录|Log_message,insertCompanyInfoDataJob.windCode={},error={}", windCode, e.getMessage(), e);
            }
        }
    }

    /**
     * 批量转档公告数据
     */
    @Test
    void insertNoticeDataJob() {
        String maxDate = simpleF9Mapper.getMaxNoticeUpdateDate();
        log.info("公告最大转档日期={}", maxDate);
        if (maxDate == null) {
            maxDate = "1970-01-01";
        }
        List<String> allWindCode = new ArrayList<>(StockCache.allWindCode);
        List<String> endWindCodeList = simpleF9Mapper.getInsertedNoticeWindCodes(maxDate);
        allWindCode.removeAll(endWindCodeList);
        log.info("公告待转档数量={}", allWindCode.size());
        for (String windCode : allWindCode) {
            F9Param f9Param = new F9Param();
            f9Param.setWindCode(windCode);
            try {
                Boolean result = simpleF9Service.insertNoticeDataJob(f9Param);
                log.info("日志记录|Log_message,insertNoticeDataJob.windCode={},result={}", windCode, result);
            } catch (Exception e) {
                if (e.getMessage().contains("403 Forbidden")) {
                    throw new RuntimeException(e.getMessage());
                }
                log.error("日志记录|Log_message,insertNoticeDataJob.windCode={},error={}", windCode, e.getMessage(), e);
            }
        }
    }

    /**
     * 批量转档大事数据
     */
    @Test
    void insertGreatEventDataJob() {
        String maxDate = simpleF9Mapper.getMaxGreatEventUpdateDate();
        log.info("大事最大转档日期={}", maxDate);
        if (maxDate == null) {
            maxDate = "1970-01-01";
        }
        List<String> allWindCode = new ArrayList<>(StockCache.allWindCode);
        List<String> endWindCodeList = simpleF9Mapper.getInsertedGreatEventWindCodes(maxDate);
        allWindCode.removeAll(endWindCodeList);
        log.info("大事待转档数量={}", allWindCode.size());
        for (String windCode : allWindCode) {
            F9Param f9Param = new F9Param();
            f9Param.setWindCode(windCode);
            try {
                Boolean result = simpleF9Service.insertGreatEventDataJob(f9Param);
                log.info("日志记录|Log_message,insertGreatEventDataJob.windCode={},result={}", windCode, result);
            } catch (Exception e) {
                if (e.getMessage().contains("403 Forbidden")) {
                    throw new RuntimeException(e.getMessage());
                }
                log.error("日志记录|Log_message,insertGreatEventDataJob.windCode={},error={}", windCode, e.getMessage(), e);
            }
        }
    }

    /**
     * 批量转档盈利预测数据
     */
    @Test
    void insertProfitForecastDataJob() {
        String maxDate = simpleF9Mapper.getMaxProfitForecastUpdateDate();
        log.info("盈利预测最大转档日期={}", maxDate);
        if (maxDate == null) {
            maxDate = "1970-01-01";
        }
        List<String> allWindCode = new ArrayList<>(StockCache.allWindCode);
        List<String> endWindCodeList = simpleF9Mapper.getInsertedProfitForecastWindCodes(maxDate);
        allWindCode.removeAll(endWindCodeList);
        log.info("盈利预测待转档数量={}", allWindCode.size());
        for (String windCode : allWindCode) {
            F9Param f9Param = new F9Param();
            f9Param.setWindCode(windCode);
            try {
                Boolean result = simpleF9Service.insertProfitForecastDataJob(f9Param);
                log.info("日志记录|Log_message,insertProfitForecastDataJob.windCode={},result={}", windCode, result);
            } catch (Exception e) {
                if (e.getMessage().contains("403 Forbidden")) {
                    throw new RuntimeException(e.getMessage());
                }
                log.error("日志记录|Log_message,insertProfitForecastDataJob.windCode={},error={}", windCode, e.getMessage(), e);
            }
        }
    }

    /**
     * 批量转档市场表现数据
     */
    @Test
    void insertMarketPerformanceDataJob() {
        String maxDate = simpleF9Mapper.getMaxMarketPerformanceUpdateDate();
        log.info("市场表现最大转档日期={}", maxDate);
        if (maxDate == null) {
            maxDate = "1970-01-01";
        }
        List<String> allWindCode = new ArrayList<>(StockCache.allWindCode);
        List<String> endWindCodeList = simpleF9Mapper.getInsertedMarketPerformanceWindCodes(maxDate);
        allWindCode.removeAll(endWindCodeList);
        log.info("市场表现待转档数量={}", allWindCode.size());
        for (String windCode : allWindCode) {
            F9Param f9Param = new F9Param();
            f9Param.setWindCode(windCode);
            try {
                Boolean result = simpleF9Service.insertMarketPerformanceDataJob(f9Param);
                log.info("日志记录|Log_message,insertMarketPerformanceDataJob.windCode={},result={}", windCode, result);
            } catch (Exception e) {
                if (e.getMessage().contains("403 Forbidden")) {
                    throw new RuntimeException(e.getMessage());
                }
                log.error("日志记录|Log_message,insertMarketPerformanceDataJob.windCode={},error={}", windCode, e.getMessage(), e);
            }
        }
    }

    /**
     * 批量转档PE_BAND数据
     */
    @Test
    void insertPeBandDataJob() {
        String maxDate = simpleF9Mapper.getMaxPeBandUpdateDate();
        log.info("PE_BAND最大转档日期={}", maxDate);
        if (maxDate == null) {
            maxDate = "1970-01-01";
        }
        List<String> allWindCode = new ArrayList<>(StockCache.allWindCode);
        List<String> endWindCodeList = simpleF9Mapper.getInsertedPeBandWindCodes(maxDate);
        allWindCode.removeAll(endWindCodeList);
        log.info("PE_BAND待转档数量={}", allWindCode.size());
        for (String windCode : allWindCode) {
            F9Param f9Param = new F9Param();
            f9Param.setWindCode(windCode);
            try {
                Boolean result = simpleF9Service.insertPeBandDataJob(f9Param);
                log.info("日志记录|Log_message,insertPeBandDataJob.windCode={},result={}", windCode, result);
            } catch (Exception e) {
                if (e.getMessage().contains("403 Forbidden")) {
                    throw new RuntimeException(e.getMessage());
                }
                log.error("日志记录|Log_message,insertPeBandDataJob.windCode={},error={}", windCode, e.getMessage(), e);
            }
        }
    }

    /**
     * 批量转档估值指标数据
     */
    @Test
    void insertSecurityMarginDataJob() {
        String maxDate = simpleF9Mapper.getMaxValuationIndexUpdateDate();
        log.info("估值指标最大转档日期={}", maxDate);
        if (maxDate == null) {
            maxDate = "1970-01-01";
        }
        List<String> allWindCode = new ArrayList<>(StockCache.allWindCode);
        List<String> endWindCodeList = simpleF9Mapper.getInsertedValuationIndexWindCodes(maxDate);
        allWindCode.removeAll(endWindCodeList);
        log.info("估值指标待转档数量={}", allWindCode.size());
        for (String windCode : allWindCode) {
            F9Param f9Param = new F9Param();
            f9Param.setWindCode(windCode);
            try {
                Boolean result = simpleF9Service.insertSecurityMarginDataJob(f9Param);
                log.info("日志记录|Log_message,insertSecurityMarginDataJob.windCode={},result={}", windCode, result);
            } catch (Exception e) {
                if (e.getMessage().contains("403 Forbidden")) {
                    throw new RuntimeException(e.getMessage());
                }
                log.error("日志记录|Log_message,insertSecurityMarginDataJob.windCode={},error={}", windCode, e.getMessage(), e);
            }
        }
    }

    /**
     * 批量转档成长能力数据
     */
    @Test
    void insertFinancialSummaryDataJob() {
        String maxDate = simpleF9Mapper.getMaxFinancialSummaryUpdateDate();
        log.info("成长能力最大转档日期={}", maxDate);
        if (maxDate == null) {
            maxDate = "1970-01-01";
        }
        List<String> allWindCode = new ArrayList<>(StockCache.allWindCode);
        List<String> endWindCodeList = simpleF9Mapper.getInsertedFinancialSummaryWindCodes(maxDate);
        allWindCode.removeAll(endWindCodeList);
        log.info("成长能力待转档数量={}", allWindCode.size());
        for (String windCode : allWindCode) {
            F9Param f9Param = new F9Param();
            f9Param.setWindCode(windCode);
            try {
                Boolean result = simpleF9Service.insertFinancialSummaryDataJob(f9Param);
                log.info("日志记录|Log_message,insertFinancialSummaryDataJob.windCode={},result={}", windCode, result);
            } catch (Exception e) {
                if (e.getMessage().contains("403 Forbidden")) {
                    throw new RuntimeException(e.getMessage());
                }
                log.error("日志记录|Log_message,insertFinancialSummaryDataJob.windCode={},error={}", windCode, e.getMessage(), e);
            }
        }
    }
}
