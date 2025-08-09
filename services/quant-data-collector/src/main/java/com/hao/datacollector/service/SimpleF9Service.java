package com.hao.datacollector.service;


import com.hao.datacollector.dto.f9.*;
import com.hao.datacollector.dto.param.f9.F9Param;

import java.util.List;

/**
 * @author LiHao
 * @description: 简版F9service
 */
public interface SimpleF9Service {
    /**
     * 获取公司简介信息
     *
     * @param lan      多语言
     * @param windCode 股票代码
     * @return 公司简介信息
     */
    CompanyProfileDTO getCompanyProfileSource(String lan, String windCode);

    /**
     * 获取资讯
     *
     * @param lan      多语言
     * @param windCode 股票代码
     * @return 资讯信息
     */
    List<InformationOceanDTO> getInformationSource(String lan, String windCode);

    /**
     * 关键统计
     *
     * @param lan      多语言
     * @param windCode 股票代码
     * @return 关键统计数据
     */
    KeyStatisticsDTO getKeyStatisticsSource(String lan, String windCode);

    /**
     * 获取公司信息
     *
     * @param lan      多语言
     * @param windCode 股票代码
     * @return 公司信息
     */
    CompanyInfo getCompanyInfoSource(String lan, String windCode);

    /**
     * 公告
     *
     * @param lan      多语言
     * @param windCode 股票代码
     * @return 公告信息
     */
    List<NoticeDTO> getNoticeSource(String lan, String windCode);

    /**
     * 大事
     *
     * @param lan      多语言
     * @param windCode 股票代码
     * @return 大事信息
     */
    List<GreatEventDTO> getGreatEventSource(String lan, String windCode);

    /**
     * 盈利预测
     *
     * @param lan      多语言
     * @param windCode 股票代码
     * @return 盈利预测
     */
    ProfitForecastDTO getProfitForecastSource(String lan, String windCode);

    /**
     * 市场表现
     *
     * @param lan      多语言
     * @param windCode 股票代码
     * @return 市场表现数据
     */
    MarketPerformanceDTO getMarketPerformanceSource(String lan, String windCode);

    /**
     * PE_BAND
     *
     * @param lan      多语言
     * @param windCode 股票代码
     * @return PE_BAND数据
     */
    List<PeBandVO> getPeBandSource(String lan, String windCode);

    /**
     * 估值指标
     *
     * @param lan      多语言
     * @param windCode 股票代码
     * @return 估值指标数据
     */
    List<ValuationIndexDTO> getSecurityMarginSource(String lan, String windCode);

    /**
     * 成长能力
     *
     * @param lan      多语言
     * @param windCode 股票代码
     * @return 成长能力数据
     */
    List<QuickViewGrowthDTO> getFinancialSummarySource(String lan, String windCode);

    /**
     * 转档公司简介信息
     *
     * @param f9Param 简版F9参数
     * @return 转档结果
     */
    Boolean insertCompanyProfileDataJob(F9Param f9Param);

    /**
     * 转档资讯信息
     *
     * @param f9Param 简版F9参数
     * @return 转档结果
     */
    Boolean insertInformationDataJob(F9Param f9Param);

    /**
     * 转档关键统计信息
     *
     * @param f9Param 简版F9参数
     * @return 转档结果
     */
    Boolean insertKeyStatisticsDataJob(F9Param f9Param);

    /**
     * 转档公司信息
     *
     * @param f9Param 简版F9参数
     * @return 转档结果
     */
    Boolean insertCompanyInfoDataJob(F9Param f9Param);

    /**
     * 转档公告信息
     *
     * @param f9Param 简版F9参数
     * @return 转档结果
     */
    Boolean insertNoticeDataJob(F9Param f9Param);

    /**
     * 转档大事信息
     *
     * @param f9Param 简版F9参数
     * @return 转档结果
     */
    Boolean insertGreatEventDataJob(F9Param f9Param);

    /**
     * 转档盈利预测信息
     *
     * @param f9Param 简版F9参数
     * @return 转档结果
     */
    Boolean insertProfitForecastDataJob(F9Param f9Param);

    /**
     * 转档市场表现信息
     *
     * @param f9Param 简版F9参数
     * @return 转档结果
     */
    Boolean insertMarketPerformanceDataJob(F9Param f9Param);

    /**
     * 转档PE_BAND信息
     *
     * @param f9Param 简版F9参数
     * @return 转档结果
     */
    Boolean insertPeBandDataJob(F9Param f9Param);

    /**
     * 转档估值指标信息
     *
     * @param f9Param 简版F9参数
     * @return 转档结果
     */
    Boolean insertSecurityMarginDataJob(F9Param f9Param);

    /**
     * 转档成长能力信息
     *
     * @param f9Param 简版F9参数
     * @return 转档结果
     */
    Boolean insertFinancialSummaryDataJob(F9Param f9Param);
}