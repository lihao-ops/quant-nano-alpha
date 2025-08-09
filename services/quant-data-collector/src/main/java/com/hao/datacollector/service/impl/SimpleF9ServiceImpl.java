package com.hao.datacollector.service.impl;


import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.TypeReference;
import com.hao.datacollector.common.constant.DataSourceConstants;
import com.hao.datacollector.common.utils.HttpUtil;
import com.hao.datacollector.dal.dao.SimpleF9Mapper;
import com.hao.datacollector.dto.f9.*;
import com.hao.datacollector.dto.param.f9.F9Param;
import com.hao.datacollector.dto.table.f9.InsertCompanyProfileDTO;
import com.hao.datacollector.service.SimpleF9Service;
import com.hao.datacollector.web.vo.result.ResultVO;
import jakarta.annotation.PostConstruct;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpHeaders;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.util.ArrayList;
import java.util.List;

/**
 * @author Hao Li
 * @description: F9实现类
 */
@Slf4j
@Service
public class SimpleF9ServiceImpl implements SimpleF9Service {
    public static String GET_COMPANY_PROFILE = "get_company_profile";
    public static String GET_INFORMATION = "get_information";
    public static String GET_KEY_STATISTICS = "get_key_statistics";
    public static String GET_COMPANY_INFO = "get_company_info";
    public static String GET_NOTICE = "get_notice";
    public static String GET_GREAT_EVENT = "get_great_event";
    public static String GET_PROFIT_FORECAST = "get_profit_forecast";
    public static String GET_MARKET_PERFORMANCE = "get_market_performance";
    public static String GET_PE_BAND = "get_pe_band";
    public static String GET_SECURITY_MARGIN = "get_security_margin";
    public static String GET_FINANCIAL_SUMMARY = "get_financial_summary";

    @Value("${wind_base.f9.base_url}")
    private String baseUrl;

    @Value("${wind_base.session_id}")
    private String sessionId;

    private static String f9BaseUlr = null;
    private static String windSessionId = null;

    @PostConstruct
    private void init() {
        windSessionId = sessionId;
        f9BaseUlr = baseUrl;
    }

    private static final Integer TIME_OUT_NUM = 10000;

    @Autowired
    private SimpleF9Mapper simpleF9Mapper;

    private static ResponseEntity<String> getF9Request(String lan, String windCode, String path) {
        String url = DataSourceConstants.WIND_PROD_WGQ + String.format(f9BaseUlr, path, lan, windCode);
        HttpHeaders headers = new HttpHeaders();
        headers.set(DataSourceConstants.WIND_SESSION_NAME, windSessionId);
        return HttpUtil.sendGet(url, headers, TIME_OUT_NUM, TIME_OUT_NUM);
    }

    /**
     * 获取公司简介信息数据源
     *
     * @param lan      多语言
     * @param windCode 股票代码
     * @return 公司简介信息
     */
    @Override
    public CompanyProfileDTO getCompanyProfileSource(String lan, String windCode) {
        ResponseEntity<String> responseEntity = getF9Request(lan, windCode, GET_COMPANY_PROFILE);
        ResultVO<CompanyProfileDTO> resultVO = null;
        try {
            resultVO = JSON.parseObject(responseEntity.getBody(), new TypeReference<ResultVO<CompanyProfileDTO>>() {
            });
        } catch (Exception e) {
            log.error("getCompanyProfile_conversion_error!={}", e.getMessage());
        }
        if (resultVO != null && resultVO.getCode() == 200 && resultVO.getData() != null) {
            return resultVO.getData();
        }
        log.error("getCompanyProfile_error={}", JSON.toJSONString(responseEntity));
        return null;
    }

    /**
     * 获取资讯数据源
     *
     * @param lan      多语言
     * @param windCode 股票代码
     * @return 资讯信息
     */
    @Override
    public List<InformationOceanDTO> getInformationSource(String lan, String windCode) {
        ResponseEntity<String> responseEntity = getF9Request(lan, windCode, GET_INFORMATION);
        ResultVO<List<InformationOceanDTO>> resultVO = null;
        try {
            resultVO = JSON.parseObject(responseEntity.getBody(), new TypeReference<ResultVO<List<InformationOceanDTO>>>() {
            });
        } catch (Exception e) {
            log.error("getInformation_conversion_error!={}", e.getMessage());
        }
        if (resultVO != null && resultVO.getCode() == 200 && resultVO.getData() != null) {
            return resultVO.getData();
        }
        log.error("getInformation_error={}", JSON.toJSONString(responseEntity));
        return List.of();
    }

    /**
     * 关键统计数据源
     *
     * @param lan      多语言
     * @param windCode 股票代码
     * @return 关键统计数据
     */
    @Override
    public KeyStatisticsDTO getKeyStatisticsSource(String lan, String windCode) {
        ResponseEntity<String> responseEntity = getF9Request(lan, windCode, GET_KEY_STATISTICS);
        ResultVO<KeyStatisticsDTO> resultVO = null;
        try {
            resultVO = JSON.parseObject(responseEntity.getBody(), new TypeReference<ResultVO<KeyStatisticsDTO>>() {
            });
        } catch (Exception e) {
            log.error("getKeyStatistics_conversion_error!={}", e.getMessage());
        }
        if (resultVO != null && resultVO.getCode() == 200 && resultVO.getData() != null) {
            return resultVO.getData();
        }
        log.error("getKeyStatistics_error={}", JSON.toJSONString(responseEntity));
        return null;
    }

    /**
     * 获取公司信息数据源
     *
     * @param lan      多语言
     * @param windCode 股票代码
     * @return 公司信息
     */
    @Override
    public CompanyInfo getCompanyInfoSource(String lan, String windCode) {
        ResponseEntity<String> responseEntity = getF9Request(lan, windCode, GET_COMPANY_INFO);
        ResultVO<CompanyInfo> resultVO = null;
        try {
            resultVO = JSON.parseObject(responseEntity.getBody(), new TypeReference<ResultVO<CompanyInfo>>() {
            });
        } catch (Exception e) {
            log.error("getCompanyInfo_conversion_error!={}", e.getMessage());
        }
        if (resultVO != null && resultVO.getCode() == 200 && resultVO.getData() != null) {
            return resultVO.getData();
        }
        log.error("getCompanyInfo_error={}", JSON.toJSONString(responseEntity));
        return null;
    }

    /**
     * 公告数据源
     *
     * @param lan      多语言
     * @param windCode 股票代码
     * @return 公告信息
     */
    @Override
    public List<NoticeDTO> getNoticeSource(String lan, String windCode) {
        ResponseEntity<String> responseEntity = getF9Request(lan, windCode, GET_NOTICE);
        ResultVO<List<NoticeDTO>> resultVO = null;
        try {
            resultVO = JSON.parseObject(responseEntity.getBody(), new TypeReference<ResultVO<List<NoticeDTO>>>() {
            });
        } catch (Exception e) {
            log.error("getNotice_conversion_error!={}", e.getMessage());
        }
        if (resultVO != null && resultVO.getCode() == 200 && resultVO.getData() != null) {
            return resultVO.getData();
        }
        log.error("getNotice_error={}", JSON.toJSONString(responseEntity));
        return List.of();
    }

    /**
     * 大事数据源
     *
     * @param lan      多语言
     * @param windCode 股票代码
     * @return 大事信息
     */
    @Override
    public List<GreatEventDTO> getGreatEventSource(String lan, String windCode) {
        ResponseEntity<String> responseEntity = getF9Request(lan, windCode, GET_GREAT_EVENT);
        ResultVO<List<GreatEventDTO>> resultVO = null;
        try {
            resultVO = JSON.parseObject(responseEntity.getBody(), new TypeReference<ResultVO<List<GreatEventDTO>>>() {
            });
        } catch (Exception e) {
            log.error("getGreatEvent_conversion_error!={}", e.getMessage());
        }
        if (resultVO != null && resultVO.getCode() == 200 && resultVO.getData() != null) {
            return resultVO.getData();
        }
        log.error("getGreatEvent_error={}", JSON.toJSONString(responseEntity));
        return List.of();
    }

    /**
     * 盈利预测数据源
     *
     * @param lan      多语言
     * @param windCode 股票代码
     * @return 盈利预测
     */
    @Override
    public ProfitForecastDTO getProfitForecastSource(String lan, String windCode) {
        ResponseEntity<String> responseEntity = getF9Request(lan, windCode, GET_PROFIT_FORECAST);
        ResultVO<ProfitForecastDTO> resultVO = null;
        try {
            resultVO = JSON.parseObject(responseEntity.getBody(), new TypeReference<ResultVO<ProfitForecastDTO>>() {
            });
        } catch (Exception e) {
            log.error("getProfitForecast_conversion_error!={}", e.getMessage());
        }
        if (resultVO != null && resultVO.getCode() == 200 && resultVO.getData() != null) {
            return resultVO.getData();
        }
        log.error("getProfitForecast_error={}", JSON.toJSONString(responseEntity));
        return null;
    }

    /**
     * 市场表现数据源
     *
     * @param lan      多语言
     * @param windCode 股票代码
     * @return 市场表现数据
     */
    @Override
    public MarketPerformanceDTO getMarketPerformanceSource(String lan, String windCode) {
        ResponseEntity<String> responseEntity = getF9Request(lan, windCode, GET_MARKET_PERFORMANCE);
        ResultVO<MarketPerformanceDTO> resultVO = null;
        try {
            resultVO = JSON.parseObject(responseEntity.getBody(), new TypeReference<ResultVO<MarketPerformanceDTO>>() {
            });
        } catch (Exception e) {
            log.error("getMarketPerformance_conversion_error!={}", e.getMessage());
        }
        if (resultVO != null && resultVO.getCode() == 200 && resultVO.getData() != null) {
            return resultVO.getData();
        }
        log.error("getMarketPerformance_error={}", JSON.toJSONString(responseEntity));
        return null;
    }

    /**
     * PE_BAND数据源
     * 记录A股日、周和月度的市盈率、市净率、市销率和市现率估值通道和估值变化
     *
     * @param lan      多语言
     * @param windCode 股票代码
     * @return PE_BAND数据
     */
    @Override
    public List<PeBandVO> getPeBandSource(String lan, String windCode) {
        ResponseEntity<String> responseEntity = getF9Request(lan, windCode, GET_PE_BAND);
        ResultVO<List<List<Object>>> resultVO = null;
        try {
            resultVO = JSON.parseObject(responseEntity.getBody(), new TypeReference<ResultVO<List<List<Object>>>>() {
            });
        } catch (Exception e) {
            log.error("getPeBand_conversion_error!={}", e.getMessage());
        }
        if (resultVO == null || resultVO.getCode() != 200 || resultVO.getData() == null) {
            return null;
        }
        //转换赋值
        List<List<Object>> resultVOData = resultVO.getData();
        List<PeBandVO> resultVOList = new ArrayList<>();
        if (resultVOData != null && resultVOData.size() > 0) {
            for (List<Object> row : resultVOData) {
                if (row.size() >= 5) {
                    PeBandVO vo = new PeBandVO();
                    vo.setDate(String.valueOf(row.get(0)));
                    vo.setClosePrice(Double.valueOf(String.valueOf(row.get(1))));
                    vo.setIndicatorValue(Double.valueOf(String.valueOf(row.get(2))));
                    vo.setAdjustmentFactor(Double.valueOf(String.valueOf(row.get(3))));
                    vo.setDrShareRatio(Double.valueOf(String.valueOf(row.get(4))));
                    resultVOList.add(vo);
                }
            }
        }
        log.info("getPeBand.size={}", resultVOList.size());
        return resultVOList;
    }

    /**
     * 估值指标数据源
     *
     * @param lan      多语言
     * @param windCode 股票代码
     * @return 估值指标数据
     */
    @Override
    public List<ValuationIndexDTO> getSecurityMarginSource(String lan, String windCode) {
        ResponseEntity<String> responseEntity = getF9Request(lan, windCode, GET_SECURITY_MARGIN);
        ResultVO<List<ValuationIndexDTO>> resultVO = null;
        try {
            resultVO = JSON.parseObject(responseEntity.getBody(), new TypeReference<ResultVO<List<ValuationIndexDTO>>>() {
            });
        } catch (Exception e) {
            log.error("getSecurityMargin_conversion_error!={}", e.getMessage());
        }
        if (resultVO != null && resultVO.getCode() == 200 && resultVO.getData() != null) {
            return resultVO.getData();
        }
        log.error("getSecurityMargin_error={}", JSON.toJSONString(responseEntity));
        return List.of();
    }

    /**
     * 成长能力数据源
     *
     * @param lan      多语言
     * @param windCode 股票代码
     * @return 成长能力数据
     */
    @Override
    public List<QuickViewGrowthDTO> getFinancialSummarySource(String lan, String windCode) {
        ResponseEntity<String> responseEntity = getF9Request(lan, windCode, GET_FINANCIAL_SUMMARY);
        ResultVO<List<QuickViewGrowthDTO>> resultVO = null;
        try {
            resultVO = JSON.parseObject(responseEntity.getBody(), new TypeReference<ResultVO<List<QuickViewGrowthDTO>>>() {
            });
        } catch (Exception e) {
            log.error("quickViewGrowthCapability_conversion_error!={}", e.getMessage());
        }
        if (resultVO != null && resultVO.getCode() == 200 && resultVO.getData() != null) {
            return resultVO.getData();
        }
        log.error("quickViewGrowthCapability_error={}", JSON.toJSONString(responseEntity));
        return List.of();
    }

    /**
     * 转档公司简介信息
     *
     * @param f9Param 简版F9参数
     * @return 转档结果
     */
    @Override
    public Boolean insertCompanyProfileDataJob(F9Param f9Param) {
        CompanyProfileDTO companyProfileSource = getCompanyProfileSource(f9Param.getLan(), f9Param.getWindCode());
        if (companyProfileSource == null || !StringUtils.hasLength(companyProfileSource.getCpyIntro())) {
            return false;
        }
        InsertCompanyProfileDTO insertCompanyProfileDTO = new InsertCompanyProfileDTO();
        BeanUtils.copyProperties(companyProfileSource, insertCompanyProfileDTO);
        insertCompanyProfileDTO.setLan(f9Param.getLan());
        insertCompanyProfileDTO.setWindCode(f9Param.getWindCode());
        //处理极大值问题
        if (insertCompanyProfileDTO.getScore().equals(Double.MAX_VALUE)) {
            insertCompanyProfileDTO.setScore(null);
        }
        List<InsertCompanyProfileDTO> insertList = new ArrayList<>();
        insertList.add(insertCompanyProfileDTO);
        int count = simpleF9Mapper.batchInsertCompanyProfileDataJob(insertList);
        log.info("insertCompanyProfileDataJob.count={}", count);
        return count >= 0;
    }

    /**
     * 转档资讯信息
     *
     * @param f9Param 简版F9参数
     * @return 转档结果
     */
    @Override
    public Boolean insertInformationDataJob(F9Param f9Param) {
        return null;
    }

    /**
     * 转档关键统计信息
     *
     * @param f9Param 简版F9参数
     * @return 转档结果
     */
    @Override
    public Boolean insertKeyStatisticsDataJob(F9Param f9Param) {
        return null;
    }

    /**
     * 转档公司信息
     *
     * @param f9Param 简版F9参数
     * @return 转档结果
     */
    @Override
    public Boolean insertCompanyInfoDataJob(F9Param f9Param) {
        return null;
    }

    /**
     * 转档公告信息
     *
     * @param f9Param 简版F9参数
     * @return 转档结果
     */
    @Override
    public Boolean insertNoticeDataJob(F9Param f9Param) {
        return null;
    }

    /**
     * 转档大事信息
     *
     * @param f9Param 简版F9参数
     * @return 转档结果
     */
    @Override
    public Boolean insertGreatEventDataJob(F9Param f9Param) {
        return null;
    }

    /**
     * 转档盈利预测信息
     *
     * @param f9Param 简版F9参数
     * @return 转档结果
     */
    @Override
    public Boolean insertProfitForecastDataJob(F9Param f9Param) {
        return null;
    }

    /**
     * 转档市场表现信息
     *
     * @param f9Param 简版F9参数
     * @return 转档结果
     */
    @Override
    public Boolean insertMarketPerformanceDataJob(F9Param f9Param) {
        return null;
    }

    /**
     * 转档PE_BAND信息
     *
     * @param f9Param 简版F9参数
     * @return 转档结果
     */
    @Override
    public Boolean insertPeBandDataJob(F9Param f9Param) {
        return null;
    }

    /**
     * 转档估值指标信息
     *
     * @param f9Param 简版F9参数
     * @return 转档结果
     */
    @Override
    public Boolean insertSecurityMarginDataJob(F9Param f9Param) {
        return null;
    }

    /**
     * 转档成长能力信息
     *
     * @param f9Param 简版F9参数
     * @return 转档结果
     */
    @Override
    public Boolean insertFinancialSummaryDataJob(F9Param f9Param) {
        return null;
    }
}