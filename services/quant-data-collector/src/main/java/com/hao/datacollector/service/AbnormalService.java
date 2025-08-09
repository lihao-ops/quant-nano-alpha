package com.hao.datacollector.service;


import com.hao.datacollector.dto.param.abnormal.IndexSourceParam;
import com.hao.datacollector.web.vo.abnormal.AbnormalIndexVO;
import com.hao.datacollector.web.vo.abnormal.ActiveRankRecordVO;
import com.hao.datacollector.web.vo.abnormal.ActiveSeatsRankVO;

import java.util.List;

/**
 * @author hli
 * @Date 2025-06-23 14:11:40
 * @description: 龙虎榜service
 */
public interface AbnormalService {
    /**
     * 获取source龙虎榜首页
     *
     * @param tradeDate 交易日期
     * @param sortCol   排序字段
     * @param orderType 排序类型
     * @return 首页VO
     * List<AbnormalIndexVO>
     */
    List<AbnormalIndexVO> getSourceHomePage(String tradeDate, Integer sortCol, Integer orderType);

    /**
     * 转档首页数据
     *
     * @param indexSourceParam 首页数据源参数对象
     * @return 操作结果
     */
    Boolean transferHomePage(IndexSourceParam indexSourceParam);


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
    List<ActiveSeatsRankVO> getSourceListOfSeats(Integer period, Integer seatType, Integer pageNo, Integer pageSize, Integer sortCol, Integer sortFlag);

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
    Boolean transferListOfSeats(Integer period, Integer seatType, Integer pageNo, Integer pageSize, Integer sortCol, Integer sortFlag);

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
    List<ActiveRankRecordVO> getSourceActiveRank(Integer period, Integer pageNo, Integer pageSize, Integer sortCol, Integer sortFlag);

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
    Boolean transferActiveRank(Integer period, Integer pageNo, Integer pageSize, Integer sortCol, Integer sortFlag);
}