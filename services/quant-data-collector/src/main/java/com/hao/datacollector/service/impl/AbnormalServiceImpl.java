package com.hao.datacollector.service.impl;


import com.alibaba.fastjson.JSONObject;
import com.alibaba.fastjson.TypeReference;
import com.alibaba.fastjson.parser.Feature;
import com.hao.datacollector.common.constant.CommonConstants;
import com.hao.datacollector.common.constant.DataSourceConstants;
import com.hao.datacollector.common.constant.DateTimeFormatConstants;
import com.hao.datacollector.common.utils.DateUtil;
import com.hao.datacollector.common.utils.ExtremeValueUtil;
import com.hao.datacollector.common.utils.HttpUtil;
import com.hao.datacollector.dal.dao.AbnormalMapper;
import com.hao.datacollector.dto.param.abnormal.IndexSourceParam;
import com.hao.datacollector.service.AbnormalService;
import com.hao.datacollector.web.vo.abnormal.AbnormalIndexVO;
import com.hao.datacollector.web.vo.abnormal.ActiveRankRecordVO;
import com.hao.datacollector.web.vo.abnormal.ActiveSeatsRankVO;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpHeaders;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;

import java.util.List;

/**
 * @author hli
 * @Date 2025-06-23 14:16:42
 * @description: 龙虎榜实现类
 */
@Slf4j
@Service
public class AbnormalServiceImpl implements AbnormalService {
    @Value("${wind_base.session_id}")
    private String windSessionId;

    @Value("${wind_base.abnormal.home_page.url}")
    private String homePageUrl;

    @Value("${wind_base.abnormal.seats.url}")
    private String seatsUrl;

    @Value("${wind_base.abnormal.active.url}")
    private String activeUrl;

    @Autowired
    private AbnormalMapper abnormalMapper;

    /**
     * 获取source龙虎榜首页
     *
     * @param tradeDate 交易日期
     * @param sortCol   排序字段
     * @param orderType 排序类型
     * @return 首页VO
     * List<AbnormalIndexVO>
     */
    @Override
    public List<AbnormalIndexVO> getSourceHomePage(String tradeDate, Integer sortCol, Integer orderType) {
        if (StringUtils.isEmpty(tradeDate)) {
            tradeDate = DateUtil.getCurrentDateTimeByStr(DateTimeFormatConstants.EIGHT_DIGIT_DATE_FORMAT);
        }
        //判断当前传入日期是否属交易日
        if (false) {
            throw new RuntimeException("not_support_tradeDate!");
        }
        HttpHeaders httpHeader = new HttpHeaders();
        httpHeader.set(DataSourceConstants.WIND_POINT_SESSION_NAME, windSessionId);
        //param
        String url = String.format(DataSourceConstants.WIND_PROD_WGQ + homePageUrl);
        MultiValueMap<String, String> queryParams = new LinkedMultiValueMap<>();
        queryParams.add("tradeDate", tradeDate);
        queryParams.add("sortCol", sortCol.toString());
        queryParams.add("orderType", orderType.toString());
        ResponseEntity<String> entity = null;
        try {
            entity = HttpUtil.sendGetWithParams(url, queryParams, httpHeader, 10000, 10000);
        } catch (Exception e) {
            throw new RuntimeException("getHomePage_error," + e.getMessage());
        }
        if (!CommonConstants.SUCCESS_FLAG.equals(entity.getStatusCode().toString())) {
            throw new RuntimeException("getHomePage_error,result=" + entity.getStatusCode());
        }
        List<AbnormalIndexVO> indexVOList = JSONObject.parseObject(entity.getBody().toString(), new TypeReference<List<AbnormalIndexVO>>() {
        }, Feature.OrderedField);
        log.info("getSourceHomePage_indexVOList.size={}", indexVOList.size());
        return indexVOList;

    }

    /**
     * 转档首页数据源
     *
     * @param indexSourceParam 首页数据源参数对象
     * @return 操作结果
     */
    @Override
    public Boolean transferHomePage(IndexSourceParam indexSourceParam) {
        List<AbnormalIndexVO> indexVOList = getSourceHomePage(indexSourceParam.getTradeDate(), indexSourceParam.getSortCol(), indexSourceParam.getOrderType());
        if (indexVOList.isEmpty()) {
            throw new RuntimeException("transferHomePage_error,indexVOList_is_empty,param=" + JSONObject.toJSONString(indexSourceParam));
        }
        int resultCount = abnormalMapper.insertHomePageSourceData(indexVOList, indexSourceParam.getTradeDate());
        log.info("transferHomePage_countResult={}", resultCount);
        return resultCount > 0;
    }

    /**
     * 获取龙虎榜席位榜数据源
     *
     * @param period   时间周期类型,0-近1月，1-近3月，2-近半年，3-今年以来
     * @param seatType 席位类型,全部席位：0，高胜率席位介入：1，机构介入：2，游资介入：3，普通介入：4
     * @param pageNo   当前页码
     * @param pageSize 每页记录数
     * @param sortCol  排序字段,0-上榜次数、1-跟投胜率、2-总成交额、3-买入次数、4-卖出次数、5-席位类型
     * @param sortFlag 排序标记,0-不排序，1-排升序，-1排降序
     * @return 席位榜数据列表
     */
    @Override
    public List<ActiveSeatsRankVO> getSourceListOfSeats(Integer period, Integer seatType, Integer pageNo, Integer pageSize, Integer sortCol, Integer sortFlag) {
        //判断当前传入日期是否属交易日
        if (false) {
            throw new RuntimeException("not_support_tradeDate!");
        }
        HttpHeaders httpHeader = new HttpHeaders();
        httpHeader.set(DataSourceConstants.WIND_POINT_SESSION_NAME, windSessionId);
        //param
        MultiValueMap<String, String> queryParams = new LinkedMultiValueMap<>();
        queryParams.add("period", period.toString());
        queryParams.add("seatType", seatType.toString());
        queryParams.add("pageNo", pageNo.toString());
        queryParams.add("pageSize", pageSize.toString());
        queryParams.add("sortCol", sortCol.toString());
        queryParams.add("sortFlag", sortFlag.toString());
        ResponseEntity<String> entity = null;
        try {
            entity = HttpUtil.sendGetWithParams(DataSourceConstants.WIND_PROD_WGQ + seatsUrl, queryParams, httpHeader, 10000, 10000);
        } catch (Exception e) {
            throw new RuntimeException("getSourceListOfSeats_error," + e.getMessage());
        }
        if (!CommonConstants.SUCCESS_FLAG.equals(entity.getStatusCode().toString())) {
            throw new RuntimeException("getSourceListOfSeats_error,result=" + entity.getStatusCode());
        }
        List<ActiveSeatsRankVO> activeSeatsRankList = JSONObject.parseObject(entity.getBody().toString(), new TypeReference<List<ActiveSeatsRankVO>>() {
        }, Feature.OrderedField);
        log.info("getSourceListOfSeats_activeSeatsRankList.size={}", activeSeatsRankList.size());
        return activeSeatsRankList;
    }

    /**
     * 转档龙虎榜席位榜数据源
     *
     * @param period   时间周期类型,0-近1月，1-近3月，2-近半年，3-今年以来
     * @param seatType 席位类型,全部席位：0，高胜率席位介入：1，机构介入：2，游资介入：3，普通介入：4
     * @param pageNo   当前页码
     * @param pageSize 每页记录数
     * @param sortCol  排序字段,0-上榜次数、1-跟投胜率、2-总成交额、3-买入次数、4-卖出次数、5-席位类型
     * @param sortFlag 排序标记,0-不排序，1-排升序，-1排降序
     * @return 操作结果
     */
    public Boolean transferListOfSeats(Integer period, Integer seatType, Integer pageNo, Integer pageSize, Integer sortCol, Integer sortFlag) {
        List<ActiveSeatsRankVO> sourceListOfSeatList = getSourceListOfSeats(period, seatType, pageNo, pageSize, sortCol, sortFlag);
        //todo 极值处理bug(非极值也处理默认值)待修复
        ExtremeValueUtil.handleExtremeValues(sourceListOfSeatList);
        int result = abnormalMapper.insertSourceListOfSeats(sourceListOfSeatList);
        return result > 0;
    }

    /**
     * 获取龙虎榜活跃榜数据源
     *
     * @param period   时间周期类型,0-近1月，1-近3月，2-近半年，3-今年以来
     * @param pageNo   当前页码
     * @param pageSize 每页记录数
     * @param sortCol  排序字段,0-上榜次数、1-跟投胜率、2-总成交额、3-买入次数、4-卖出次数、5-席位类型、6-涨跌幅
     * @param sortFlag 排序标记,0-不排序，1-排升序，-1排降序
     * @return 活跃榜数据列表
     */
    @Override
    public List<ActiveRankRecordVO> getSourceActiveRank(Integer period, Integer pageNo, Integer pageSize, Integer sortCol, Integer sortFlag) {
        //判断当前传入日期是否属交易日
        if (false) {
            throw new RuntimeException("not_support_tradeDate!");
        }
        HttpHeaders httpHeader = new HttpHeaders();
        httpHeader.set(DataSourceConstants.WIND_POINT_SESSION_NAME, windSessionId);
        //param
        MultiValueMap<String, String> queryParams = new LinkedMultiValueMap<>();
        queryParams.add("period", period.toString());
        queryParams.add("pageNo", pageNo.toString());
        queryParams.add("pageSize", pageSize.toString());
        queryParams.add("sortCol", sortCol.toString());
        queryParams.add("sortFlag", sortFlag.toString());
        ResponseEntity<String> entity = null;
        try {
            entity = HttpUtil.sendGetWithParams(DataSourceConstants.WIND_PROD_WGQ + activeUrl, queryParams, httpHeader, 10000, 10000);
        } catch (Exception e) {
            throw new RuntimeException("getSourceActiveRank_error," + e.getMessage());
        }
        if (!CommonConstants.SUCCESS_FLAG.equals(entity.getStatusCode().toString())) {
            throw new RuntimeException("getSourceActiveRank_error,result=" + entity.getStatusCode());
        }
        List<ActiveRankRecordVO> activeRankRecordVOList = JSONObject.parseObject(entity.getBody().toString(), new TypeReference<List<ActiveRankRecordVO>>() {
        }, Feature.OrderedField);
        log.info("getSourceActiveRank_activeRankRecordVOList.size={}", activeRankRecordVOList.size());
        return activeRankRecordVOList;
    }

    /**
     * 转档龙虎榜活跃榜数据源
     *
     * @param period   时间周期类型,0-近1月，1-近3月，2-近半年，3-今年以来
     * @param pageNo   当前页码
     * @param pageSize 每页记录数
     * @param sortCol  排序字段,0-上榜次数、1-跟投胜率、2-总成交额、3-买入次数、4-卖出次数、5-席位类型、6-涨跌幅
     * @param sortFlag 排序标记,0-不排序，1-排升序，-1排降序
     * @return 操作结果
     */
    @Override
    public Boolean transferActiveRank(Integer period, Integer pageNo, Integer pageSize, Integer sortCol, Integer sortFlag) {
        List<ActiveRankRecordVO> activeRankVOList = getSourceActiveRank(period, pageNo, pageSize, sortCol, sortFlag);
        //todo 极值处理bug(非极值也处理默认值)待修复
        ExtremeValueUtil.handleExtremeValues(activeRankVOList);
        int result = abnormalMapper.insertActiveRankVOList(activeRankVOList);
        return result > 0;
    }
}