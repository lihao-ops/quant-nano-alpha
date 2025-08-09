package com.hao.datacollector.dal.dao;

import com.hao.datacollector.dto.param.stock.StockBasicInfoQueryParam;
import com.hao.datacollector.dto.param.stock.StockMarketDataQueryParam;
import com.hao.datacollector.dto.table.base.StockBaseDTO;
import com.hao.datacollector.dto.table.base.StockBasicInfoInsertDTO;
import com.hao.datacollector.dto.table.base.StockDailyMetricsDTO;
import com.hao.datacollector.dto.table.base.StockFinancialMetricsInsertDTO;
import com.hao.datacollector.web.vo.stock.StockBasicInfoQueryResultVO;
import com.hao.datacollector.web.vo.stock.StockMarketDataQueryResultVO;
import org.apache.ibatis.annotations.Param;

import java.time.LocalDate;
import java.util.List;

public interface BaseDataMapper {
    /**
     * 批量插入股票财务指标信息
     *
     * @param list 一个包含多个股票财务指标插入数据传输对象的列表
     * @return 如果插入操作成功，则返回true；否则返回false
     */
    Boolean batchInsertStockFinancialMetrics(List<StockFinancialMetricsInsertDTO> list);

    /**
     * 批量插入股票基本信息
     *
     * @param list 一个包含多个股票基本信息插入数据传输对象的列表
     * @return 如果插入操作成功，则返回true；否则返回false
     */
    Boolean batchInsertStockBasicInfo(List<StockBasicInfoInsertDTO> list);

    /**
     * 获取所有股票的WindCode
     *
     * @return 包含所有A股票证券代码的列表
     */
    List<String> getAllAStockCode();

    /**
     * 获取所有股票的股票code+name
     *
     * @return 列表
     */
    List<StockBaseDTO> getAllStockBaseInfo();

    /**
     * 批量插入股票行情数据
     * <p>
     * 此方法旨在接收一个包含股票每日指标数据的列表，并将这些数据批量插入数据库或数据存储系统中
     * 它主要用于处理股票市场数据的批量上传、数据初始化或数据同步等场景
     *
     * @param list 一个包含多个StockDailyMetricsDTO对象的列表，每个对象代表股票在某一天的指标数据
     * @return 如果批量插入操作成功，则返回true；否则返回false
     */
    Boolean batchInsertStockMarketData(List<StockDailyMetricsDTO> list);

    /**
     * 获取在特定交易日期已插入成功的代码列表
     * <p>
     * 此方法旨在检索和提供在给定交易日期内，数据库或系统中未记录的市场代码信息
     * 主要用于数据完整性检查、审计或同步过程中识别缺失的数据
     *
     * @param startDate 起始交易日期，用于查询未插入市场的代码的特定日期
     * @param endDate   截止交易日期
     * @return 已插入市场的代码列表，每个代码作为字符串提供
     */
    List<String> getInsertMarketCode(@Param("startDate") String startDate, @Param("endDate") String endDate);

    /**
     * 清空交易日历表数据
     *
     * @return 操作结果
     */
    Integer clearTradeDate();

    /**
     * 插入交易日历数据
     *
     * @param tradeDateList 交易日历列表
     * @return 操作结果
     */
    Boolean insertTradeDate(@Param("list") List<LocalDate> tradeDateList);

    /**
     * 根据时间区间获取交易日历
     *
     * @param startTime 起始日期
     * @param endTime   结束日期
     * @return 交易日历
     */
    List<String> getTradeDateListByTime(String startTime, String endTime);

    /**
     * 查询股票基本信息
     *
     * @param queryParam 股票基本信息查询参数
     * @return 股票基本信息列表
     */
    List<StockBasicInfoQueryResultVO> queryStockBasicInfo(StockBasicInfoQueryParam queryParam);

    /**
     * 查询股票行情数据
     *
     * @param queryParam 股票行情数据查询参数
     * @return 股票行情数据列表
     */
    List<StockMarketDataQueryResultVO> queryStockMarketData(StockMarketDataQueryParam queryParam);

    /**
     * 插入异常股票
     *
     * @param windCode 股票代码
     * @return 操作结果
     */
    boolean insertAbnormalStock(String windCode);

    /**
     * 获取异常股票列表
     *
     * @return 异常股票列表
     */
    List<String> getAbnormalStockList();
}
