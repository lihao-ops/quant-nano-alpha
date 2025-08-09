package com.hao.datacollector.service;

import com.hao.datacollector.dto.param.stock.StockBasicInfoQueryParam;
import com.hao.datacollector.dto.param.stock.StockMarketDataQueryParam;
import com.hao.datacollector.dto.table.base.StockDailyMetricsDTO;
import com.hao.datacollector.web.vo.stock.StockBasicInfoQueryResultVO;
import com.hao.datacollector.web.vo.stock.StockMarketDataQueryResultVO;

import java.io.File;
import java.time.LocalDate;
import java.util.List;

public interface BaseDataService {

    /**
     * 批量插入股票基本信息
     * <p>
     * 该方法通过读取指定文件中的股票基本信息，并将其批量插入数据库或其他持久化存储中
     * 主要用于初始化股票基本信息数据库，或更新大量股票基本信息
     *
     * @param file 包含股票基本信息的文件，文件格式和内容需符合特定规范
     * @return 插入操作是否成功如果所有信息都成功插入，返回true；否则返回false
     */
    Boolean batchInsertStockBasicInfo(File file);

    /**
     * 批量插入股票行情指标数据
     * <p>
     * 该方法主要用于批量插入指定时间段内的股票财务指标数据它通过接收一个股票代码列表
     * 以及开始和结束时间，来查询并插入数据这种方法适用于需要一次性插入大量数据的场景，
     * 可以提高数据插入的效率和性能
     *
     * @param startTime 插入数据的开始时间，格式为"YYYY-MM-DD"
     * @param endTime   插入数据的结束时间，格式为"YYYY-MM-DD"
     * @return 返回一个Boolean值，表示数据插入是否成功如果插入成功，返回true；否则返回false
     */
    Boolean batchInsertStockMarketData(String startTime, String endTime);

    /**
     * 将CSV行字符串转换为股票每日指标DTO列表
     * 此方法主要用于处理CSV文件中的一行数据，将其解析并转换成一个或多个股票每日指标数据传输对象（DTO）的列表
     * CSV行中的每个记录按照预定的格式映射到StockDailyMetricsDTO的相应字段
     *
     * @param csvLine 包含股票每日指标数据的CSV行字符串，各个数据项之间使用逗号分隔
     * @return 解析后的股票每日指标DTO列表，如果CSV行为空或格式不正确，可能返回空列表或null
     */
    List<StockDailyMetricsDTO> convert(String csvLine);

    /**
     * 获取并插入指定时间段的股票市场数据
     * <p>
     * 该方法用于处理股票市场数据的获取和插入操作它根据给定的股票代码和时间范围，
     * 从数据源（例如数据库或API）获取股票每日指标数据，并将其插入或更新在系统中
     *
     * @param windCode  股票代码，用于标识特定的股票
     * @param startTime 开始时间，指定获取数据的起始日期
     * @param endTime   结束时间，指定获取数据的终止日期
     * @return 返回插入操作的结果
     */
    List<StockDailyMetricsDTO> getInsertStockMarketData(String windCode, String startTime, String endTime);

    /**
     * 转档交易日期
     *
     * @param startTime 起始日期
     * @param endTime   结束日期
     * @return 转档结果
     */
    Boolean setTradeDateList(String startTime, String endTime);

    /**
     * 根据时间区间获取交易日历
     *
     * @param startTime 起始日期
     * @param endTime   结束日期
     * @return 交易日历
     */
    List<LocalDate> getTradeDateListByTime(String startTime, String endTime);

    /**
     * 查询股票基本信息
     * <p>
     * 该方法用于根据查询条件获取股票基本信息数据，支持多种查询条件组合
     * 包括股票代码、股票名称、交易所、行业分类、上市日期等条件的筛选
     * 支持分页查询，提高查询效率和用户体验
     *
     * @param queryParam 股票基本信息查询参数，包含各种筛选条件和分页信息
     * @return 返回符合条件的股票基本信息列表
     */
    List<StockBasicInfoQueryResultVO> queryStockBasicInfo(StockBasicInfoQueryParam queryParam);

    /**
     * 查询股票行情数据
     * <p>
     * 该方法用于根据查询条件获取股票行情指标数据，支持多种查询条件组合
     * 包括股票代码、交易日期范围、价格区间、成交量区间等条件的筛选
     * 支持分页查询，适用于大量历史行情数据的查询场景
     *
     * @param queryParam 股票行情数据查询参数，包含各种筛选条件和分页信息
     * @return 返回符合条件的股票行情数据列表
     */
    List<StockMarketDataQueryResultVO> queryStockMarketData(StockMarketDataQueryParam queryParam);
}

