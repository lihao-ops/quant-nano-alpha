package com.hao.datacollector.web.controller;

import com.hao.datacollector.dto.f9.*;
import com.hao.datacollector.dto.param.f9.F9Param;
import com.hao.datacollector.service.SimpleF9Service;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.Parameters;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * @author Hao Li
 * @description: 简版F9
 */
@Tag(name = "简版F9")
@RestController
@RequestMapping("/f9")
public class SimpleF9Controller {

    @Autowired
    private SimpleF9Service simpleF9Service;

    @Operation(summary = "获取公司简介信息数据源")
    @Parameters({
            @Parameter(name = "lan", description = "多语言(默认:CN)", required = false),
            @Parameter(name = "windCode", description = "股票代码", required = true)
    })
    @GetMapping("/get_company_profile_source")
    public CompanyProfileDTO getCompanyProfileSource(@RequestParam(required = false, defaultValue = "CN") String lan, String windCode) {
        return simpleF9Service.getCompanyProfileSource(lan, windCode);
    }

    @Operation(summary = "获取资讯数据源")
    @Parameters({
            @Parameter(name = "lan", description = "多语言(默认:CN)", required = false),
            @Parameter(name = "windCode", description = "股票代码", required = true)
    })
    @GetMapping("/get_information_source")
    public List<InformationOceanDTO> getInformationSource(@RequestParam(required = false, defaultValue = "CN") String lan, String windCode) {
        return simpleF9Service.getInformationSource(lan, windCode);
    }

    @Operation(summary = "获取关键统计数据源")
    @Parameters({
            @Parameter(name = "lan", description = "多语言(默认:CN)", required = false),
            @Parameter(name = "windCode", description = "股票代码", required = true)
    })
    @GetMapping("/get_key_statistics_source")
    public KeyStatisticsDTO getKeyStatisticsSource(@RequestParam(required = false, defaultValue = "CN") String lan, String windCode) {
        return simpleF9Service.getKeyStatisticsSource(lan, windCode);
    }

    @Operation(summary = "获取公司信息数据源")
    @Parameters({
            @Parameter(name = "lan", description = "多语言(默认:CN)", required = false),
            @Parameter(name = "windCode", description = "股票代码", required = true)
    })
    @GetMapping("/get_company_info_source")
    public CompanyInfo getCompanyInfoSource(@RequestParam(required = false, defaultValue = "CN") String lan, String windCode) {
        return simpleF9Service.getCompanyInfoSource(lan, windCode);
    }

    @Operation(summary = "获取公告数据源")
    @Parameters({
            @Parameter(name = "lan", description = "多语言(默认:CN)", required = false),
            @Parameter(name = "windCode", description = "股票代码", required = true)
    })
    @GetMapping("/get_notice_source")
    public List<NoticeDTO> getNoticeSource(@RequestParam(required = false, defaultValue = "CN") String lan, String windCode) {
        return simpleF9Service.getNoticeSource(lan, windCode);
    }

    @Operation(summary = "获取大事数据源")
    @Parameters({
            @Parameter(name = "lan", description = "多语言(默认:CN)", required = false),
            @Parameter(name = "windCode", description = "股票代码", required = true)
    })
    @GetMapping("/get_great_event_source")
    public List<GreatEventDTO> getGreatEventSource(@RequestParam(required = false, defaultValue = "CN") String lan, String windCode) {
        return simpleF9Service.getGreatEventSource(lan, windCode);
    }

    @Operation(summary = "获取盈利预测数据源")
    @Parameters({
            @Parameter(name = "lan", description = "多语言(默认:CN)", required = false),
            @Parameter(name = "windCode", description = "股票代码", required = true)
    })
    @GetMapping("/get_profit_forecast_source")
    public ProfitForecastDTO getProfitForecastSource(@RequestParam(required = false, defaultValue = "CN") String lan, String windCode) {
        return simpleF9Service.getProfitForecastSource(lan, windCode);
    }

    @Operation(summary = "获取市场表现数据源")
    @Parameters({
            @Parameter(name = "lan", description = "多语言(默认:CN)", required = false),
            @Parameter(name = "windCode", description = "股票代码", required = true)
    })
    @GetMapping("/get_market_performance_source")
    public MarketPerformanceDTO getMarketPerformanceSource(@RequestParam(required = false, defaultValue = "CN") String lan, String windCode) {
        return simpleF9Service.getMarketPerformanceSource(lan, windCode);
    }

    @Operation(summary = "获取PE_BAND数据源")
    @Parameters({
            @Parameter(name = "lan", description = "多语言(默认:CN)", required = false),
            @Parameter(name = "windCode", description = "股票代码", required = true)
    })
    @GetMapping("/get_pe_band_source")
    public List<PeBandVO> getPeBandSource(@RequestParam(required = false, defaultValue = "CN") String lan, String windCode) {
        return simpleF9Service.getPeBandSource(lan, windCode);
    }

    @Operation(summary = "获取估值指标数据源")
    @Parameters({
            @Parameter(name = "lan", description = "多语言(默认:CN)", required = false),
            @Parameter(name = "windCode", description = "股票代码", required = true)
    })
    @GetMapping("/get_security_margin_source")
    public List<ValuationIndexDTO> getSecurityMarginSource(@RequestParam(required = false, defaultValue = "CN") String lan, String windCode) {
        return simpleF9Service.getSecurityMarginSource(lan, windCode);
    }

    @Operation(summary = "获取成长能力数据源")
    @Parameters({
            @Parameter(name = "lan", description = "多语言(默认:CN)", required = false),
            @Parameter(name = "windCode", description = "股票代码", required = true)
    })
    @GetMapping("/get_financial_summary_source")
    public List<QuickViewGrowthDTO> getFinancialSummarySource(@RequestParam(required = false, defaultValue = "CN") String lan, String windCode) {
        return simpleF9Service.getFinancialSummarySource(lan, windCode);
    }

    @Operation(summary = "转档公司简介信息",
            description = "将公司简介数据进行转档处理",
            method = "POST")
    @PostMapping("/company_profile_job")
    public Boolean insertCompanyProfileDataJob(
            @Parameter(description = "包含公司简介转档所需的参数信息", required = true)
            @RequestBody F9Param f9Param) {
        return simpleF9Service.insertCompanyProfileDataJob(f9Param);
    }

    @Operation(summary = "转档资讯信息",
            description = "将资讯数据进行转档处理",
            method = "POST")
    @PostMapping("/information_job")
    public Boolean insertInformationDataJob(
            @Parameter(description = "包含资讯转档所需的参数信息", required = true)
            @RequestBody F9Param f9Param) {
        return simpleF9Service.insertInformationDataJob(f9Param);
    }

    @Operation(summary = "转档关键统计信息",
            description = "将关键统计数据进行转档处理",
            method = "POST")
    @PostMapping("/key_statistics_job")
    public Boolean insertKeyStatisticsDataJob(
            @Parameter(description = "包含关键统计转档所需的参数信息", required = true)
            @RequestBody F9Param f9Param) {
        return simpleF9Service.insertKeyStatisticsDataJob(f9Param);
    }

    @Operation(summary = "转档公司信息",
            description = "将公司信息数据进行转档处理",
            method = "POST")
    @PostMapping("/company_info_job")
    public Boolean insertCompanyInfoDataJob(
            @Parameter(description = "包含公司信息转档所需的参数信息", required = true)
            @RequestBody F9Param f9Param) {
        return simpleF9Service.insertCompanyInfoDataJob(f9Param);
    }

    @Operation(summary = "转档公告信息",
            description = "将公告数据进行转档处理",
            method = "POST")
    @PostMapping("/notice_job")
    public Boolean insertNoticeDataJob(
            @Parameter(description = "包含公告转档所需的参数信息", required = true)
            @RequestBody F9Param f9Param) {
        return simpleF9Service.insertNoticeDataJob(f9Param);
    }

    @Operation(summary = "转档大事信息",
            description = "将大事数据进行转档处理",
            method = "POST")
    @PostMapping("/great_event_job")
    public Boolean insertGreatEventDataJob(
            @Parameter(description = "包含大事转档所需的参数信息", required = true)
            @RequestBody F9Param f9Param) {
        return simpleF9Service.insertGreatEventDataJob(f9Param);
    }

    @Operation(summary = "转档盈利预测信息",
            description = "将盈利预测数据进行转档处理",
            method = "POST")
    @PostMapping("/profit_forecast_job")
    public Boolean insertProfitForecastDataJob(
            @Parameter(description = "包含盈利预测转档所需的参数信息", required = true)
            @RequestBody F9Param f9Param) {
        return simpleF9Service.insertProfitForecastDataJob(f9Param);
    }

    @Operation(summary = "转档市场表现信息",
            description = "将市场表现数据进行转档处理",
            method = "POST")
    @PostMapping("/market_performance_job")
    public Boolean insertMarketPerformanceDataJob(
            @Parameter(description = "包含市场表现转档所需的参数信息", required = true)
            @RequestBody F9Param f9Param) {
        return simpleF9Service.insertMarketPerformanceDataJob(f9Param);
    }

    @Operation(summary = "转档PE_BAND信息",
            description = "将PE_BAND数据进行转档处理",
            method = "POST")
    @PostMapping("/pe_band_job")
    public Boolean insertPeBandDataJob(
            @Parameter(description = "包含PE_BAND转档所需的参数信息", required = true)
            @RequestBody F9Param f9Param) {
        return simpleF9Service.insertPeBandDataJob(f9Param);
    }

    @Operation(summary = "转档估值指标信息",
            description = "将估值指标数据进行转档处理",
            method = "POST")
    @PostMapping("/security_margin_job")
    public Boolean insertSecurityMarginDataJob(
            @Parameter(description = "包含估值指标转档所需的参数信息", required = true)
            @RequestBody F9Param f9Param) {
        return simpleF9Service.insertSecurityMarginDataJob(f9Param);
    }

    @Operation(summary = "转档成长能力信息",
            description = "将成长能力数据进行转档处理",
            method = "POST")
    @PostMapping("/financial_summary_job")
    public Boolean insertFinancialSummaryDataJob(
            @Parameter(description = "包含成长能力转档所需的参数信息", required = true)
            @RequestBody F9Param f9Param) {
        return simpleF9Service.insertFinancialSummaryDataJob(f9Param);
    }
}