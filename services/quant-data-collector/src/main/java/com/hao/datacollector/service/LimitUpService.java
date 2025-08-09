package com.hao.datacollector.service;

import com.hao.datacollector.dto.param.limitup.LimitUpStockQueryParam;
import com.hao.datacollector.web.vo.limitup.ApiResponse;
import com.hao.datacollector.web.vo.limitup.LimitUpStockQueryResultVO;
import com.hao.datacollector.web.vo.limitup.ResultObjectVO;

import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * @author hli
 * @program: datacollector
 * @Date 2025-06-12 19:34:31
 * @description: 涨停相关接口
 */
public interface LimitUpService {

    /**
     * 获取解析后的涨停选股接口数据
     *
     * @param tradeTime 交易日期
     * @return 解析结果对象
     */
    ApiResponse<ResultObjectVO> getLimitUpData(String tradeTime);

    /**
     * 将涨停数据转档到数据库
     *
     * @param tradeTime 交易日期
     * @return 是否成功
     */
    Boolean transferLimitUpDataToDatabase(String tradeTime);

    /**
     * 查询涨停股票信息列表
     *
     * @param queryParam 查询参数
     * @return 结果列表
     */
    List<LimitUpStockQueryResultVO> queryLimitUpStockList(LimitUpStockQueryParam queryParam);

    /**
     * 获取交易日涨停股票代码列表
     *
     * @param tradeDateStart 交易日期开始
     * @param tradeDateEnd   交易日期结束
     * @return 交易日涨停股票代码列表
     */
    Map<String, Set<String>> getLimitUpTradeDateMap(String tradeDateStart, String tradeDateEnd);
}
