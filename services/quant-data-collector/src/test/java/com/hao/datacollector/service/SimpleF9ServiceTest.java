package com.hao.datacollector.service;

import com.alibaba.fastjson.JSON;
import com.hao.datacollector.common.cache.StockCache;
import com.hao.datacollector.dal.dao.SimpleF9Mapper;
import com.hao.datacollector.dto.f9.*;
import com.hao.datacollector.dto.param.f9.F9Param;
import lombok.extern.slf4j.Slf4j;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import java.util.ArrayList;
import java.util.List;

/**
 * @author Hao Li
 * @description:
 */
@Slf4j
@SpringBootTest
public class SimpleF9ServiceTest {
    @Autowired
    private SimpleF9Service simpleF9Service;

    @Autowired
    private SimpleF9Mapper simpleF9Mapper;

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
                log.info("insertCompanyProfileDataJob.windCode={},result={}", windCode, result);
            } catch (Exception e) {
                if (e.getMessage().contains("403 Forbidden")) {
                    throw new RuntimeException(e.getMessage());
                }
                log.error("insertCompanyProfileDataJob.windCode={},error={}", windCode, e.getMessage());
            }
        }
    }

    @Test
    void getCompanyProfileSource() {
        CompanyProfileDTO companyProfile = simpleF9Service.getCompanyProfileSource("cn", "600519.SH");
        log.info(JSON.toJSONString(companyProfile));
    }

    @Test
    void getInformationSource() {
        List<InformationOceanDTO> information = simpleF9Service.getInformationSource("cn", "600519.SH");
        log.info(JSON.toJSONString(information));
    }

    @Test
    void getKeyStatisticsSource() {
        KeyStatisticsDTO keyStatistics = simpleF9Service.getKeyStatisticsSource("cn", "600519.SH");
        log.info(JSON.toJSONString(keyStatistics));
    }

    @Test
    void getCompanyInfoSource() {
        CompanyInfo companyInfo = simpleF9Service.getCompanyInfoSource("cn", "600519.SH");
        log.info(JSON.toJSONString(companyInfo));
    }

    @Test
    void getNoticeSource() {
        List<NoticeDTO> notice = simpleF9Service.getNoticeSource("cn", "600519.SH");
        log.info(JSON.toJSONString(notice));
    }

    @Test
    void getGreatEventSource() {
        List<GreatEventDTO> greatEvent = simpleF9Service.getGreatEventSource("cn", "600519.SH");
        log.info(JSON.toJSONString(greatEvent));
    }

    @Test
    void getProfitForecastSource() {
        ProfitForecastDTO profitForecast = simpleF9Service.getProfitForecastSource("cn", "600519.SH");
        log.info(JSON.toJSONString(profitForecast));
    }

    @Test
    void getMarketPerformanceSource() {
        MarketPerformanceDTO marketPerformance = simpleF9Service.getMarketPerformanceSource("cn", "600519.SH");
        log.info(JSON.toJSONString(marketPerformance));
    }

    @Test
    void getPeBandSource() {
        List<PeBandVO> peBand = simpleF9Service.getPeBandSource("cn", "600519.SH");
        log.info(JSON.toJSONString(peBand));
    }

    @Test
    void getSecurityMarginSource() {
        List<ValuationIndexDTO> securityMargin = simpleF9Service.getSecurityMarginSource("cn", "600519.SH");
        log.info(JSON.toJSONString(securityMargin));
    }

    @Test
    void getFinancialSummarySource() {
        List<QuickViewGrowthDTO> quickViewGrowthCapability = simpleF9Service.getFinancialSummarySource("cn", "600519.SH");
        log.info(JSON.toJSONString(quickViewGrowthCapability));
    }
}