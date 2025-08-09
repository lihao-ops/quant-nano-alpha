package com.hao.datacollector.dal.dao;

import com.hao.datacollector.dto.quotation.HistoryTrendDTO;
import com.hao.datacollector.dto.table.quotation.QuotationStockBaseDTO;
import org.apache.ibatis.annotations.Param;

import java.util.List;

public interface QuotationMapper {
    /**
     * 批量插入基础行情数据
     *
     * @param quotationStockBaseList 行情数据列表
     * @return 插入数量
     */
    int insertQuotationStockBaseList(@Param("baseQuotationList") List<QuotationStockBaseDTO> quotationStockBaseList);

    /**
     * 获取指定时间内已转档的股票列表
     *
     * @param startDate 开始时间
     * @param endDate   结束时间
     * @return 已转档的股票列表
     */
    List<String> getJobQuotationBaseEndWindCodeList(@Param("startDate") String startDate, @Param("endDate") String endDate);

    /**
     * 批量插入历史分时行情数据
     *
     * @param historyTrendQuotationList 历史分时行情数据列表
     * @return 插入数量
     */
    int insertQuotationHistoryTrendList(@Param("historyTrendQuotationList") List<HistoryTrendDTO> historyTrendQuotationList);

    /**
     * 获取指定年份的股票历史分时数据结束日期
     *
     * @param year 年份 yyyy
     * @return 当前年份最大日期yyyyMMdd
     */
    String getMaxHistoryTrendEndDate(@Param("year") String year);

    /**
     * 获取指定日期已完成的股票列表
     *
     * @param maxEndDate 最大结束日期
     * @return 已完成的股票列表
     */
    List<String> getCompletedWindCodes(String maxEndDate);
}
