package com.hao.datacollector.service.impl;

import com.alibaba.fastjson.JSON;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.hao.datacollector.common.constant.DataSourceConstants;
import com.hao.datacollector.common.constant.DateTimeFormatConstants;
import com.hao.datacollector.common.utils.*;
import com.hao.datacollector.dal.dao.BaseDataMapper;
import com.hao.datacollector.dto.param.stock.StockBasicInfoQueryParam;
import com.hao.datacollector.dto.param.stock.StockMarketDataQueryParam;
import com.hao.datacollector.dto.table.base.StockBasicInfoInsertDTO;
import com.hao.datacollector.dto.table.base.StockDailyMetricsDTO;
import com.hao.datacollector.dto.table.base.StockFinancialMetricsInsertDTO;
import com.hao.datacollector.service.BaseDataService;
import com.hao.datacollector.web.vo.result.ResultVO;
import com.hao.datacollector.web.vo.stock.StockBasicInfoQueryResultVO;
import com.hao.datacollector.web.vo.stock.StockMarketDataQueryResultVO;
import com.wind.api.W;
import com.wind.api.struct.WindData;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.http.HttpHeaders;
import org.springframework.stereotype.Service;

import java.io.File;
import java.math.BigDecimal;
import java.sql.Date;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Function;
import java.util.function.Predicate;
import java.util.stream.Collectors;

import static com.hao.datacollector.common.utils.ExcelReaderUtil.readHeaders;

/**
 * @author hli
 * @program: datacollector
 * @Date 2025-06-02 17:06:23
 * @description: 基础数据处理实现类
 */
@Slf4j
@Service
public class BaseDataServiceImpl implements BaseDataService {

    @Value("${wind_base.session_id}")
    private String windSessionId;

    /**
     * 交易日历url
     */
    @Value("${wind_base.trade.date.url}")
    private String tradeDateBaseUrl;

    @Autowired
    private BaseDataMapper baseDataMapper;

    // 添加Jackson ObjectMapper
    private final ObjectMapper objectMapper = new ObjectMapper();

    /**
     * 批量插入股票基本信息
     * 该方法读取Excel文件中的股票数据，并将其插入数据库中
     * 主要包括股票的基本信息和财务指标
     *
     * @param file 包含股票信息的Excel文件
     * @return 插入操作是否成功，成功返回true，否则返回false
     */
    @Override
    public Boolean batchInsertStockBasicInfo(File file) {
        // 读取Excel文件的表头信息
        ExcelToDtoConverter.headers = readHeaders(file);
        // 读取Excel文件的数据并转换为列表
        List<Map<String, String>> dataList = ExcelReaderUtil.readExcel(file);
        // 转 DTO
        List<StockBasicInfoInsertDTO> basicInfoList = ExcelToDtoConverter.convertToBasicInfoDTO(dataList);
        List<StockFinancialMetricsInsertDTO> metricsList = ExcelToDtoConverter.convertToFinancialMetricsDTO(dataList);
        // 批量插入数据到数据库
        Boolean financialMetricsInsertResult = baseDataMapper.batchInsertStockFinancialMetrics(metricsList);
        Boolean basicInfoInsertResult = baseDataMapper.batchInsertStockBasicInfo(basicInfoList);
        log.info("BaseDataServiceImpl_batchInsertStockBasicInfo_financialMetricsInsertResult={},basicInfoInsertResult={}", financialMetricsInsertResult, basicInfoInsertResult);
        return financialMetricsInsertResult && basicInfoInsertResult;
    }

    /**
     * 批量插入指定时间范围内的股票市场数据
     *
     * @param startTime 开始时间，格式为字符串
     * @param endTime   结束时间，格式为字符串
     * @return 返回是否成功插入所有数据
     * <p>
     * 本方法通过遍历所有A股代码，获取每个代码在指定时间范围内的市场数据，并将其插入数据库
     * 如果在获取数据过程中遇到错误，会将已获取的数据先插入数据库，然后重新开始获取剩余的数据
     */
    public Boolean batchInsertStockMarketData(String startTime, String endTime) {
        //登录
        loginW();
        // 获取所有A股的代码
        List<String> allWindCode = baseDataMapper.getAllAStockCode();
        //清理已经插入过的代码,无需重复插入。
        String endDate = DateUtil.stringTimeToAdjust(endTime, DateTimeFormatConstants.DEFAULT_DATE_FORMAT, 1);
        List<String> overInsertMarketCode = baseDataMapper.getInsertMarketCode(startTime, endDate);
        allWindCode.removeAll(overInsertMarketCode);
        //清理异常的股票列表
        List<String> abnormalStockList = baseDataMapper.getAbnormalStockList();
        allWindCode.removeAll(abnormalStockList);
        int emptyStatus = 0;
        // 如果中断遍历时,先将已转化的DTOList插入数据库,后查询插入数据库的所有代码,从AllCode中剔除,就是还需要获取的剩余数据。继续获取
        for (int i = 0; i < allWindCode.size(); i++) {
            // 获取单个股票的市场数据
            List<StockDailyMetricsDTO> stockDailyMetricsList = getInsertStockMarketData(allWindCode.get(i), startTime, endTime);
            // 如果获取的数据连续三次为空，表示出现调用异常
            if (stockDailyMetricsList.isEmpty()) {
                if (emptyStatus == 500) {
                    log.error("已处理共计{}个代码数据", i);
                    throw new RuntimeException("继续获取股票市场数据失败");
                }
                boolean insertAbnormalResult = baseDataMapper.insertAbnormalStock(allWindCode.get(i));
                log.error("BaseDataServiceImpl_batchInsertStockMarketData_windCode={},insertAbnormalResult={}", allWindCode.get(i), insertAbnormalResult);
                emptyStatus += 1;
                continue;
            }
            //日期去重
            Boolean insertStockMarketData = baseDataMapper.batchInsertStockMarketData(stockDailyMetricsList.stream().filter(distinctByKey(StockDailyMetricsDTO::getTradeDate)).collect(Collectors.toList()));
            log.info("batchInsertStockMarketData_code={}!stockDailyMetricsList.size={},insertStockMarketData={}", allWindCode.get(i), stockDailyMetricsList.size(), insertStockMarketData);
        }
        return true;
    }

    // 辅助去重方法
    public static <T> Predicate<T> distinctByKey(Function<? super T, ?> keyExtractor) {
        Set<Object> seen = ConcurrentHashMap.newKeySet();
        return t -> seen.add(keyExtractor.apply(t));
    }

    /**
     * 登录Wind
     */
    public void loginW() {
        //登录
        long start = W.start();
        //获取版本
        WindData version = W.getVersion();
        log.info("BaseDataServiceImpl_login_version={}", version);
    }

    public List<StockDailyMetricsDTO> getInsertStockMarketData(String windCode, String startTime, String endTime) {
        /**
         * lastradeday_s:当前交易日期
         * windcode:证券代码
         * sec_name:证券简称
         * latestconcept:所属最新概念
         * chain:所属产业链板块
         * esg_rating_wind:ESG评级
         * open:开盘价
         * high:最高价
         * low:最低价
         * close:收盘价
         * vwap:均价
         * volume_btin:成交量(含大宗交易)/股
         * amount_btin:成交额(含大宗交易)/元
         * pct_chg:涨跌幅
         * turn:换手率
         * free_turn:换手率(基准:自由流通股本)
         * maxup:涨停价
         * maxdown:跌停价
         * trade_status:交易状态
         * ev:总市值1
         * mkt_freeshares:自由流通市值
         * open_auction_price:开盘集合竞价成交价
         * open_auction_volume:开盘集合竞价成交量
         * open_auction_amount:开盘集合竞价成交额
         * mfd_buyamt_at:主动买入额(全单)/元
         * mfd_sellamt_at:主动卖出额(全单)/元
         * mfd_buyvol_at:主动买入量(全单)/股
         * mfd_sellvol_at:主动卖出量(全单)/股
         *
         * tech_turnoverrate5:5日平均换手率
         * tech_turnoverrate10:10日平均换手率
         * mfd_inflow_m:主力净流入额(元)
         * mfd_inflowproportion_m:主力净流入额占比
         */
        WindData wsd = W.wsd(windCode, "lastradeday_s,windcode,sec_name,latestconcept,chain,esg_rating_wind,open,high,low,close,vwap,volume_btin,amount_btin,pct_chg,turn,free_turn,maxup,maxdown,trade_status,ev,mkt_freeshares,open_auction_price,open_auction_volume,open_auction_amount,mfd_buyamt_at,mfd_sellamt_at,mfd_buyvol_at,mfd_sellvol_at,tech_turnoverrate5,tech_turnoverrate10,mfd_inflow_m,mfd_inflowproportion_m", "2025-01-01", "2025-03-01", "");
        if (wsd.getErrorId() != 0) {
            throw new RuntimeException("getInsertStockMarketData_error,wsd.ErrorId=" + wsd.getErrorId());
        }
        return convert(wsd.getData().toString().replace("[", "").replace("]", ""));
    }


    public List<StockDailyMetricsDTO> convert(String csvLine) {
        List<StockDailyMetricsDTO> insertDataDTOList = new ArrayList<>();
        boolean statusFlag = false;
        try {
            String[] parts = csvLine.split(", ", -1); // 支持空值
            for (int index = 0; index < parts.length - 1; ) {
                StockDailyMetricsDTO dto = new StockDailyMetricsDTO();
                //当前交易日数据为null则表示可能为新股此时并无交易数据,且只打印一次
                if ("null".equals(parts[index].trim())) {
                    index += 32;
                    if (index == 0) {
                        // 在第188行附近，将JSON.toJSONString替换为Jackson
                        try {
                            log.error("疑是新股数据,跳过={}", objectMapper.writeValueAsString(parts));
                        } catch (JsonProcessingException e) {
                            log.error("疑是新股数据,跳过={}", java.util.Arrays.toString(parts));
                        }
                    }
                    continue;
                }
                //第一次到正常值的时候index无需+1
                if (!statusFlag) {
                    statusFlag = true;
                } else {
                    index = index == 0 ? 0 : index + 1;
                }
                dto.setTradeDate(Date.valueOf(parts[index].trim().split(" ")[0]));
                dto.setWindcode(parts[index += 1].trim().equals("null") ? null : parts[index].trim());
                dto.setSecName(parts[index += 1].trim().equals("null") ? null : parts[index].trim());
                dto.setLatestconcept(parts[index += 1].trim().equals("null") ? null : parts[index].trim());
                dto.setChain(parts[index += 1].trim().equals("null") ? null : parts[index].trim());
                dto.setEsgRatingWind(parts[index += 1].trim().equals("null") ? null : parts[index].trim());

                dto.setOpen(new BigDecimal(parts[index += 1].trim().equals("null") ? "0" : parts[index].trim()));
                dto.setHigh(new BigDecimal(parts[index += 1].trim().equals("null") ? "0" : parts[index].trim()));
                dto.setLow(new BigDecimal(parts[index += 1].trim().equals("null") ? "0" : parts[index].trim()));
                dto.setClose(new BigDecimal(parts[index += 1].trim().equals("null") ? "0" : parts[index].trim()));
                dto.setVwap(new BigDecimal(parts[index += 1].trim().equals("null") ? "0" : parts[index].trim()));

                //11
                dto.setVolumeBtin((long) Double.parseDouble(parts[index += 1].trim().equals("null") ? "0" : parts[index].trim()));
                dto.setAmountBtin(new BigDecimal(parts[index += 1].trim().equals("null") ? "0" : parts[index].trim()));
                dto.setPctChg(new BigDecimal(parts[index += 1].trim().equals("null") ? "0" : parts[index].trim()));
                dto.setTurn(new BigDecimal(parts[index += 1].trim().equals("null") ? "0" : parts[index].trim()));
                dto.setFreeTurn(new BigDecimal(parts[index += 1].trim().equals("null") ? "0" : parts[index].trim()));
                dto.setMaxup(new BigDecimal(parts[index += 1].trim().equals("null") ? "0" : parts[index].trim()));
                dto.setMaxdown(new BigDecimal(parts[index += 1].trim().equals("null") ? "0" : parts[index].trim()));
                dto.setTradeStatus(parts[index += 1].trim().equals("null") ? "0" : parts[index].trim());

                dto.setEv(new BigDecimal(parts[index += 1].trim().equals("null") ? "0" : parts[index].trim()));
                dto.setMktFreeshares(new BigDecimal(parts[index += 1].trim().equals("null") ? "0" : parts[index].trim()));

                dto.setOpenAuctionPrice(new BigDecimal(parts[index += 1].trim().equals("null") ? "0" : parts[index].trim()));
                dto.setOpenAuctionVolume((long) Double.parseDouble(parts[index += 1].trim().equals("null") ? "0" : parts[index].trim()));
                dto.setOpenAuctionAmount(new BigDecimal(parts[index += 1].trim().equals("null") ? "0" : parts[index].trim()));
                dto.setMfdBuyamtAt(new BigDecimal(parts[index += 1].trim().equals("null") ? "0" : parts[index].trim()));
                dto.setMfdSellamtAt(new BigDecimal(parts[index += 1].trim().equals("null") ? "0" : parts[index].trim()));
                dto.setMfdBuyvolAt((long) Double.parseDouble(parts[index += 1].trim().equals("null") ? "0" : parts[index].trim()));
                dto.setMfdSellvolAt((long) Double.parseDouble(parts[index += 1].trim().equals("null") ? "0" : parts[index].trim()));
                dto.setTechTurnoverrate5(new BigDecimal(parts[index += 1].trim().equals("null") ? "0" : parts[index].trim()));
                dto.setTechTurnoverrate10(new BigDecimal(parts[index += 1].trim().equals("null") ? "0" : parts[index].trim()));
                dto.setMfdInflowM(new BigDecimal(parts[index += 1].trim().equals("null") ? "0" : parts[index].trim()));
                dto.setMfdInflowproportionM(new BigDecimal(parts[index += 1].trim().equals("null") ? "0" : parts[index].trim()));
                insertDataDTOList.add(dto);
            }
        } catch (Exception e) {
            e.printStackTrace();
            log.error("CSV转换失败={}", e.getMessage());
            return null;
        }
        log.info("BaseDataServiceImpl_convert_insertDataDTOList.size={}", insertDataDTOList.size());
        return insertDataDTOList;
    }

    /**
     * 转档交易日期
     *
     * @param startTime 起始日期
     * @param endTime   结束日期
     * @return 转档结果
     */
    @Override
    public Boolean setTradeDateList(String startTime, String endTime) {
        String requestTradeDateUrl = String.format(tradeDateBaseUrl, startTime, endTime);
        HttpHeaders headers = new HttpHeaders();
        headers.set(DataSourceConstants.WIND_SESSION_NAME, windSessionId);
        String response = HttpUtil.sendGetRequest(DataSourceConstants.WIND_PROD_WGQ + requestTradeDateUrl, headers, 10000, 30000).getBody();
        // 解析JSON响应为LimitResultVO对象
        // 配置忽略未知字段，避免反序列化错误
        objectMapper.configure(com.fasterxml.jackson.databind.DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);
        List<Integer> tradeDateList = null;
        try {
            //List<String>，格式为 yyyyMMdd
            ResultVO<List<Integer>> result = objectMapper.readValue(
                    response,
                    objectMapper.getTypeFactory().constructParametricType(
                            ResultVO.class,
                            List.class
                    )
            );
            tradeDateList = result.getData();
        } catch (Exception e) {
            log.error("BaseDataServiceImpl_setTradeDateList_tradeDateList_parse_error,response={}", e.getMessage(), response);
            throw new RuntimeException("BaseDataServiceImpl_setTradeDateList_tradeDateList_parse_error");
        }
        if (tradeDateList.isEmpty()) {
            throw new RuntimeException("BaseDataServiceImpl_setTradeDateList_tradeDateList_isEmpty");
        }
        //先清空再插入
        Integer tradeDate = baseDataMapper.clearTradeDate();
        Boolean clearTradeDateResult = tradeDate >= 0;
        // 将其转为 LocalDate 格式
        List<LocalDate> dateList = tradeDateList.stream()
                .map(i -> LocalDate.parse(String.valueOf(i), DateTimeFormatter.ofPattern(DateTimeFormatConstants.EIGHT_DIGIT_DATE_FORMAT)))
                .collect(Collectors.toList());
        Boolean insertTradeDateListResult = baseDataMapper.insertTradeDate(dateList);
        log.info("BaseDataServiceImpl_setTradeDateList,tradeDateList.size={},clearTradeDateResult={},insertTradeDateListResult={}", tradeDateList.size(), clearTradeDateResult, insertTradeDateListResult);
        return clearTradeDateResult && insertTradeDateListResult;
    }

    /**
     * 根据时间区间获取交易日历
     *
     * @param startTime 起始日期
     * @param endTime   结束日期
     * @return 交易日历
     */
    @Override
//    @Cacheable(cacheNames = "tradeDateListByTime", key = "#startTime + #endTime", cacheManager = "dateCaffeineCacheManager")
    public List<LocalDate> getTradeDateListByTime(String startTime, String endTime) {
        List<String> listByTime = baseDataMapper.getTradeDateListByTime(startTime, endTime);
        log.info("getTradeDateListByTime,listByTime.size={}", listByTime.size());
        //转换为通用的LocalDate提供自由日期格式转换
        return listByTime.stream()
                .map(LocalDate::parse) // 默认是 yyyy-MM-dd 格式
                .collect(Collectors.toList());
    }

    /**
     * 查询股票基本信息
     *
     * @param queryParam 股票基本信息查询参数
     * @return 股票基本信息列表
     */
    @Override
    public List<StockBasicInfoQueryResultVO> queryStockBasicInfo(StockBasicInfoQueryParam queryParam) {
        log.info("BaseDataServiceImpl_queryStockBasicInfo_start=queryParam={}", JSON.toJSONString(queryParam));
        // 处理分页参数，将pageNo转换为offset
        if (queryParam.getPageNo() != null && queryParam.getPageSize() != null) {
            int offset = PageUtil.calculateOffset(queryParam.getPageNo(), queryParam.getPageSize());
            queryParam.setPageNo(offset);
        }
        List<StockBasicInfoQueryResultVO> result = baseDataMapper.queryStockBasicInfo(queryParam);
        log.info("BaseDataServiceImpl_queryStockBasicInfo_success=result_count={}", result.size());
        return result;
    }

    /**
     * 查询股票行情数据
     *
     * @param queryParam 股票行情数据查询参数
     * @return 股票行情数据列表
     */
    @Override
    public List<StockMarketDataQueryResultVO> queryStockMarketData(StockMarketDataQueryParam queryParam) {
        log.info("BaseDataServiceImpl_queryStockMarketData_start=queryParam={}", JSON.toJSONString(queryParam));
        // 处理分页参数，将pageNo转换为offset
        if (queryParam.getPageNo() != null && queryParam.getPageSize() != null) {
            int offset = PageUtil.calculateOffset(queryParam.getPageNo(), queryParam.getPageSize());
            queryParam.setPageNo(offset);
        }
        List<StockMarketDataQueryResultVO> result = baseDataMapper.queryStockMarketData(queryParam);
        log.info("BaseDataServiceImpl_queryStockMarketData_success=result_count={}", result.size());
        return result;
    }
}
