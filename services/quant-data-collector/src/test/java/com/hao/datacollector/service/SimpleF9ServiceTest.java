package com.hao.datacollector.service;

import com.hao.datacollector.cache.StockCache;
import com.hao.datacollector.dal.dao.SimpleF9Mapper;
import com.hao.datacollector.dto.f9.*;
import com.hao.datacollector.dto.param.f9.F9Param;
import lombok.extern.slf4j.Slf4j;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import util.JsonUtil;

import java.util.ArrayList;
import java.util.List;

/**
 * F9接口数据采集测试类
 *
 * 测试目的：
 * 1. 验证F9接口数据获取流程是否正常。
 * 2. 验证批量转档逻辑在真实数据下的稳定性。
 *
 * 设计思路：
 * - 基于Spring Boot容器注入服务与Mapper。
 * - 使用固定股票代码输出JSON结果用于人工核对。
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
     *
     * 实现逻辑：
     * 1. 获取未入库的Wind代码列表。
     * 2. 逐条调用转档接口并记录结果。
     * 3. 捕获异常并输出失败原因。
     */
    @Test
    void insertCompanyProfileDataJob() {
        List<String> allWindCode = new ArrayList<>(StockCache.allWindCode);
        List<String> endWindCodeList = simpleF9Mapper.getInsertFinancialSummaryData();
        allWindCode.removeAll(endWindCodeList);
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

    /**
     * 查询公司简介数据源
     *
     * 实现逻辑：
     * 1. 调用服务获取公司简介数据。
     * 2. 输出JSON结果用于核对。
     */
    @Test
    void getCompanyProfileSource() {
        CompanyProfileDTO companyProfile = simpleF9Service.getCompanyProfileSource("cn", "600519.SH");
        log.info("日志记录|Log_message,companyProfile={}", JsonUtil.toJson(companyProfile));
    }

    /**
     * 查询资讯数据源
     *
     * 实现逻辑：
     * 1. 调用服务获取资讯列表。
     * 2. 输出JSON结果用于核对。
     */
    @Test
    void getInformationSource() {
        List<InformationOceanDTO> information = simpleF9Service.getInformationSource("cn", "600519.SH");
        log.info("日志记录|Log_message,information={}", JsonUtil.toJson(information));
    }

    /**
     * 查询关键统计数据源
     *
     * 实现逻辑：
     * 1. 调用服务获取关键统计数据。
     * 2. 输出JSON结果用于核对。
     */
    @Test
    void getKeyStatisticsSource() {
        KeyStatisticsDTO keyStatistics = simpleF9Service.getKeyStatisticsSource("cn", "600519.SH");
        log.info("日志记录|Log_message,keyStatistics={}", JsonUtil.toJson(keyStatistics));
    }

    /**
     * 查询公司信息数据源
     *
     * 实现逻辑：
     * 1. 调用服务获取公司信息。
     * 2. 输出JSON结果用于核对。
     */
    @Test
    void getCompanyInfoSource() {
        CompanyInfo companyInfo = simpleF9Service.getCompanyInfoSource("cn", "600519.SH");
        log.info("日志记录|Log_message,companyInfo={}", JsonUtil.toJson(companyInfo));
    }

    /**
     * 查询公告数据源
     *
     * 实现逻辑：
     * 1. 调用服务获取公告列表。
     * 2. 输出JSON结果用于核对。
     */
    @Test
    void getNoticeSource() {
        List<NoticeDTO> notice = simpleF9Service.getNoticeSource("cn", "600519.SH");
        log.info("日志记录|Log_message,notice={}", JsonUtil.toJson(notice));
    }

    /**
     * 查询大事数据源
     *
     * 实现逻辑：
     * 1. 调用服务获取大事列表。
     * 2. 输出JSON结果用于核对。
     */
    @Test
    void getGreatEventSource() {
        List<GreatEventDTO> greatEvent = simpleF9Service.getGreatEventSource("cn", "600519.SH");
        log.info("日志记录|Log_message,greatEvent={}", JsonUtil.toJson(greatEvent));
    }

    /**
     * 查询盈利预测数据源
     *
     * 实现逻辑：
     * 1. 调用服务获取盈利预测数据。
     * 2. 输出JSON结果用于核对。
     */
    @Test
    void getProfitForecastSource() {
        ProfitForecastDTO profitForecast = simpleF9Service.getProfitForecastSource("cn", "600519.SH");
        log.info("日志记录|Log_message,profitForecast={}", JsonUtil.toJson(profitForecast));
    }

    /**
     * 查询市场表现数据源
     *
     * 实现逻辑：
     * 1. 调用服务获取市场表现数据。
     * 2. 输出JSON结果用于核对。
     */
    @Test
    void getMarketPerformanceSource() {
        MarketPerformanceDTO marketPerformance = simpleF9Service.getMarketPerformanceSource("cn", "600519.SH");
        log.info("日志记录|Log_message,marketPerformance={}", JsonUtil.toJson(marketPerformance));
    }

    /**
     * 查询估值带数据源
     *
     * 实现逻辑：
     * 1. 调用服务获取估值带列表。
     * 2. 输出JSON结果用于核对。
     */
    @Test
    void getPeBandSource() {
        List<PeBandVO> peBand = simpleF9Service.getPeBandSource("cn", "600519.SH");
        log.info("日志记录|Log_message,peBand={}", JsonUtil.toJson(peBand));
    }

    /**
     * 查询安全边际数据源
     *
     * 实现逻辑：
     * 1. 调用服务获取安全边际数据。
     * 2. 输出JSON结果用于核对。
     */
    @Test
    void getSecurityMarginSource() {
        List<ValuationIndexDTO> securityMargin = simpleF9Service.getSecurityMarginSource("cn", "600519.SH");
        log.info("日志记录|Log_message,securityMargin={}", JsonUtil.toJson(securityMargin));
    }

    /**
     * 查询财务摘要数据源
     *
     * 实现逻辑：
     * 1. 调用服务获取财务摘要数据。
     * 2. 输出JSON结果用于核对。
     */
    @Test
    void getFinancialSummarySource() {
        List<QuickViewGrowthDTO> quickViewGrowthCapability = simpleF9Service.getFinancialSummarySource("cn", "600519.SH");
        log.info("日志记录|Log_message,quickViewGrowthCapability={}", JsonUtil.toJson(quickViewGrowthCapability));
    }
}
