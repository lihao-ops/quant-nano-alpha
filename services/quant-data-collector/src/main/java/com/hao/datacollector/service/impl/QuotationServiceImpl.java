package com.hao.datacollector.service.impl;

import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONObject;
import com.alibaba.fastjson.TypeReference;
import com.hao.datacollector.common.utils.HttpUtil;
import com.hao.datacollector.dal.dao.QuotationMapper;
import com.hao.datacollector.dto.quotation.HistoryTrendDTO;
import com.hao.datacollector.dto.quotation.HistoryTrendIndexDTO;
import com.hao.datacollector.dto.table.quotation.QuotationStockBaseDTO;
import com.hao.datacollector.properties.DataCollectorProperties;
import com.hao.datacollector.service.QuotationService;
import constants.DataSourceConstants;
import constants.DateTimeFormatConstants;
import enums.SpeedIndicatorEnum;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpHeaders;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;
import util.DateUtil;
import util.MathUtil;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;

/**
 * @author hli
 * @program: datacollector
 * @Date 2025-07-04 17:43:47
 * @description: 行情实现类
 */
@Slf4j
@Service
public class QuotationServiceImpl implements QuotationService {

    @Autowired
    private DataCollectorProperties properties;

    @Value("${wind_base.quotation.base.url}")
    private String QuotationBaseUrl;

    @Value("${wind_base.quotation.history.trend.url}")
    private String QuotationHistoryTrendUrl;

    @Autowired
    private QuotationMapper quotationMapper;

    /**
     * 请求成功标识
     */
    private static final String SUCCESS_FLAG = "200 OK";

    /**
     * 获取基础行情数据
     *
     * @param windCode  股票代码
     * @param startDate 起始日期
     * @param endDate   结束日期
     * @return 当前股票基础行情数据
     */
    @Override
    public Boolean transferQuotationBaseByStock(String windCode, String startDate, String endDate) {
        HttpHeaders headers = new HttpHeaders();
        headers.add(DataSourceConstants.WIND_POINT_SESSION_NAME, properties.getWindSessionId());
        String url = DataSourceConstants.WIND_PROD_WGQ + String.format(QuotationBaseUrl, windCode, startDate, endDate);
        ResponseEntity<String> response = HttpUtil.sendGetRequest(url, headers, 30000, 30000);
        List<List<Long>> quotationList = JSON.parseObject(response.getBody(), new TypeReference<List<List<Long>>>() {
        });
        List<QuotationStockBaseDTO> quotationStockBaseList = new ArrayList<>();
        if (quotationList == null || quotationList.isEmpty()) {
            log.error("quotationData.quotationList_isEmpty()!windCode={}", windCode);
            return false;
        }
        for (List<Long> quotationData : quotationList) {
            if (quotationData.isEmpty()) {
                log.error("quotationData.isEmpty()!windCode={}", windCode);
                continue;
            }
            QuotationStockBaseDTO quotationStockBaseDTO = new QuotationStockBaseDTO();
            quotationStockBaseDTO.setWindCode(windCode);
            quotationStockBaseDTO.setTradeDate(DateUtil.parseToLocalDate(String.valueOf(quotationData.get(0)), DateTimeFormatConstants.EIGHT_DIGIT_DATE_FORMAT));
            //元
            quotationStockBaseDTO.setOpenPrice(MathUtil.shiftDecimal(quotationData.get(1).toString(), 2));
            //元
            quotationStockBaseDTO.setHighPrice(MathUtil.shiftDecimal(quotationData.get(2).toString(), 2));
            //元
            quotationStockBaseDTO.setLowPrice(MathUtil.shiftDecimal(quotationData.get(3).toString(), 2));
            //手
            quotationStockBaseDTO.setVolume(MathUtil.shiftDecimal(quotationData.get(4).toString(), 2));
            //元
            quotationStockBaseDTO.setAmount(MathUtil.shiftDecimal(quotationData.get(5).toString(), 0));
            //元
            quotationStockBaseDTO.setClosePrice(MathUtil.shiftDecimal(quotationData.get(6).toString(), 2));
            //%
            quotationStockBaseDTO.setTurnoverRate(MathUtil.shiftDecimal(quotationData.get(7).toString(), 2));
            quotationStockBaseList.add(quotationStockBaseDTO);
        }
        if (quotationStockBaseList.isEmpty()) {
            log.error("transferQuotationBaseByStock_list=null!,windCode={}", windCode);
            return false;
        }
        int insertResult = quotationMapper.insertQuotationStockBaseList(quotationStockBaseList);
        return insertResult > 0;
    }

    /**
     * 转档股票历史分时数据
     *
     * @param tradeDate 交易日期,如:20220608
     * @param windCodes 股票代码List
     * @param dateType  时间类型,0表示固定时间
     * @return 操作结果
     */
    @Override
    public Boolean transferQuotationHistoryTrend(int tradeDate, String windCodes, Integer dateType) {
        List<HistoryTrendDTO> quotationHistoryTrendList = getQuotationHistoryTrendList(tradeDate, windCodes, dateType);
        if (quotationHistoryTrendList.isEmpty()) {
            log.error("quotationHistoryTrendList.isEmpty()!tradeDate={},windCodes={},dateType={}", tradeDate, windCodes, dateType);
            return false;
        }
        int insertResult = quotationMapper.insertQuotationHistoryTrendList(quotationHistoryTrendList);
        return insertResult > 0;
    }

    /**
     * 转档指标历史分时数据
     *
     * @param tradeDate 交易日期,如:20220608
     * @param windCodes 股票代码List
     * @param dateType  时间类型,0表示固定时间
     * @return 操作结果
     */
    @Override
    public Boolean transferQuotationIndexHistoryTrend(int tradeDate, String windCodes, Integer dateType) {
        List<HistoryTrendIndexDTO> quotationHistoryIndexTrendList = getQuotationHistoryIndexTrendList(tradeDate, windCodes, dateType);
        if (quotationHistoryIndexTrendList.isEmpty()) {
            log.error("quotationHistoryIndexTrendList.isEmpty()!tradeDate={},windCodes={},dateType={}", tradeDate, windCodes, dateType);
            return false;
        }
        int insertResult = quotationMapper.insertQuotationIndexHistoryTrendList(quotationHistoryIndexTrendList);
        return insertResult > 0;
    }

    /**
     * 获取股票历史分时对象List
     *
     * @param tradeDate 交易日期,如:20220608
     * @param windCodes 股票代码List
     * @param dateType  时间类型,0表示固定时间
     * @return 操作结果
     */
    private List<HistoryTrendDTO> getQuotationHistoryTrendList(int tradeDate, String windCodes, Integer dateType) {
        HttpHeaders headers = new HttpHeaders();
        headers.add(DataSourceConstants.WIND_POINT_SESSION_NAME, properties.getWindSessionId());
        String url = DataSourceConstants.WIND_PROD_WGQ + String.format(QuotationHistoryTrendUrl, tradeDate, windCodes, dateType);
        int retryCount = 0;
        int maxRetries = 2; // 最多重试2次
        ResponseEntity<String> response = null;
        while (retryCount <= maxRetries) {
            try {
                response = HttpUtil.sendGet(url, headers, 100000, 100000);
                break; // 成功则跳出循环
            } catch (Exception ex) {
                retryCount++;
                if (retryCount > maxRetries) {
                    // 超过最大重试次数，尝试从下一个ip获取
                    continue;
                }
                // 重试前等待一段时间
                try {
                    Thread.sleep(1000 * retryCount); // 递增等待时间
                } catch (InterruptedException ie) {
                    Thread.currentThread().interrupt();
                }
            }
        }
        Map<String, Map<String, Object>> rawData = JSON.parseObject(response.getBody(), new TypeReference<Map<String, Map<String, Object>>>() {
        });
        List<HistoryTrendDTO> allHistoryTrendList = new ArrayList<>();
        for (Map.Entry<String, Map<String, Object>> stockEntry : rawData.entrySet()) {
            String stockCode = stockEntry.getKey();
            Map<String, Object> stockData = stockEntry.getValue();
            for (Map.Entry<String, Object> dateEntry : stockData.entrySet()) {
                List<HistoryTrendDTO> historyTrendList = new ArrayList<>();
                String date = dateEntry.getKey();
                Object dateData = dateEntry.getValue();
                if (dateData == null || dateData.getClass().equals(Integer.class)) {
                    continue;
                }
                List<List<Integer>> dataArrays = (List<List<Integer>>) dateData;
                if (dataArrays.isEmpty()) continue;
                // 获取配置数组,只获取一次,indicatorIds.对应指标id元素下标数组,decimalShifts.对应关于每个位置元素精度小数位数(倒序)
                List<Integer> indicatorIds = new ArrayList<>();
                List<Integer> decimalShifts = new ArrayList<>();
                List<Integer> configArray = dataArrays.get(dataArrays.size() - 1);
                for (int i = 0; i < 5; i++) {
                    indicatorIds.add(configArray.get(i));
                    decimalShifts.add(configArray.get(i + 5));
                }
                Collections.reverse(decimalShifts);
                int time_s = 0;
                Double latestPrice = 0.00, averagePrice = 0.00;
                int sum = dataArrays.stream()
                        .filter(subList -> subList.size() > 1)
                        .mapToInt(subList -> subList.get(1))
                        .sum();
                if (sum > 160000) {
                    throw new RuntimeException("数据异常");
                }
                int timeIndex = indicatorIds.indexOf(2);
                int latestPriceIndex = indicatorIds.indexOf(3);
                int averagePriceIndex = indicatorIds.indexOf(79);
                int totalVolumeIndex = indicatorIds.indexOf(8);
                //剔除最后一行指标数据
                for (int i = 0; i < dataArrays.size() - 1; i++) {
                    HistoryTrendDTO historyTrendDTO = new HistoryTrendDTO();
                    time_s += dataArrays.get(i).get(timeIndex).intValue();
                    // 转换为LocalDateTime
                    LocalDate localDateTime = LocalDate.parse(date, DateTimeFormatter.ofPattern("yyyyMMdd"));
                    LocalTime time = LocalTime.of(
                            time_s / 10000,        // 小时: 13
                            (time_s % 10000) / 100, // 分钟: 38
                            time_s % 100           // 秒: 58
                    );
                    LocalDateTime dateTime = LocalDateTime.of(localDateTime, time);
                    historyTrendDTO.setTradeDate(dateTime);
                    historyTrendDTO.setWindCode(stockCode);
                    historyTrendDTO.setLatestPrice(latestPrice += dataArrays.get(i).get(latestPriceIndex));
                    historyTrendDTO.setAveragePrice(averagePrice += dataArrays.get(i).get(averagePriceIndex));
                    //总成交量不需要累加
                    historyTrendDTO.setTotalVolume(Double.valueOf(dataArrays.get(i).get(totalVolumeIndex)));
                    historyTrendList.add(historyTrendDTO);
                }
                //精度处理
                for (HistoryTrendDTO historyTrendDTO : historyTrendList) {
                    historyTrendDTO.setLatestPrice(MathUtil.formatDecimal(historyTrendDTO.getLatestPrice(), decimalShifts.get(latestPriceIndex), false));
                    historyTrendDTO.setAveragePrice(MathUtil.formatDecimal(historyTrendDTO.getAveragePrice(), decimalShifts.get(averagePriceIndex), false));
                    //成交额(买卖都算),由于A股市场都是以100股为单位/1手,故此在此固定/100
                    historyTrendDTO.setTotalVolume(MathUtil.formatDecimal(historyTrendDTO.getTotalVolume(), 2, false));
                }
                allHistoryTrendList.addAll(historyTrendList);
            }
        }
        log.info("getQuotationHistoryTrendList_allHistoryTrendList.size={}", allHistoryTrendList.size());
        return allHistoryTrendList;
    }

    /**
     * 获取指标历史分时对象List
     *
     * @param tradeDate 交易日期,如:20220608
     * @param windCodes 股票代码List
     * @param dateType  时间类型,0表示固定时间
     * @return 操作结果
     */
    private List<HistoryTrendIndexDTO> getQuotationHistoryIndexTrendList(int tradeDate, String windCodes, Integer dateType) {
        HttpHeaders headers = new HttpHeaders();
        headers.add(DataSourceConstants.WIND_POINT_SESSION_NAME, properties.getWindSessionId());
        String url = DataSourceConstants.WIND_PROD_WGQ + String.format(QuotationHistoryTrendUrl, tradeDate, windCodes, dateType);
        int retryCount = 0;
        int maxRetries = 2; // 最多重试2次
        ResponseEntity<String> response = null;
        while (retryCount <= maxRetries) {
            try {
                response = HttpUtil.sendGet(url, headers, 100000, 100000);
                break; // 成功则跳出循环
            } catch (Exception ex) {
                retryCount++;
                if (retryCount > maxRetries) {
                    break; // 超过最大重试次数，退出循环
                }
                // 重试前等待一段时间
                try {
                    Thread.sleep(1000 * retryCount); // 递增等待时间
                } catch (InterruptedException ie) {
                    Thread.currentThread().interrupt();
                    throw new RuntimeException("请求被中断", ie);
                }
            }
        }
        if (response == null || response.getBody() == null) {
            throw new RuntimeException("请求失败，无法获取数据");
        }
        // 处理响应数据结构 - 先解析外层包装
        String responseBody = response.getBody();
        String actualDataJson = responseBody;
        try {
            // 检查是否有外层包装
            JSONObject wrapper = JSON.parseObject(responseBody);
            if (wrapper != null && wrapper.containsKey("body")) {
                Object bodyObj = wrapper.get("body");
                if (bodyObj instanceof String) {
                    actualDataJson = (String) bodyObj;
                } else {
                    actualDataJson = JSON.toJSONString(bodyObj);
                }
            }
        } catch (Exception e) {
            // 如果解析失败，使用原始响应体
            log.warn("解析外层包装失败，使用原始响应体: {}", e.getMessage());
        }
        // 解析实际数据
        Map<String, Map<String, Object>> rawData;
        try {
            rawData = JSON.parseObject(actualDataJson, new TypeReference<Map<String, Map<String, Object>>>() {
            });
        } catch (Exception e) {
            log.error("解析JSON数据失败: {}", e.getMessage());
            throw new RuntimeException("数据解析失败", e);
        }
        if (rawData == null || rawData.isEmpty()) {
            log.warn("解析后的数据为空");
            return new ArrayList<>();
        }
        List<HistoryTrendIndexDTO> allHistoryIndexTrendList = new ArrayList<>();
        for (Map.Entry<String, Map<String, Object>> stockEntry : rawData.entrySet()) {
            String stockCode = stockEntry.getKey();
            Map<String, Object> stockData = stockEntry.getValue();
            if (stockData == null) {
                continue;
            }
            for (Map.Entry<String, Object> dateEntry : stockData.entrySet()) {
                String date = dateEntry.getKey();
                Object dateData = dateEntry.getValue();
                // 跳过空数据或非数组数据
                if (dateData == null || dateData instanceof Integer) {
                    continue;
                }
                // 安全转换为List<List<Number>>
                List<List<Number>> dataArrays;
                try {
                    dataArrays = JSON.parseObject(JSON.toJSONString(dateData), new TypeReference<List<List<Number>>>() {
                    });
                } catch (Exception e) {
                    log.warn("转换数据数组失败, stockCode: {}, date: {}, error: {}", stockCode, date, e.getMessage());
                    continue;
                }
                if (dataArrays == null || dataArrays.isEmpty()) {
                    continue;
                }

                try {
                    List<HistoryTrendIndexDTO> historyIndexTrendList = processStockDateData(stockCode, date, dataArrays);
                    allHistoryIndexTrendList.addAll(historyIndexTrendList);
                } catch (Exception e) {
                    log.error("处理股票数据失败, stockCode: {}, date: {}, error: {}", stockCode, date, e.getMessage());
                    // 继续处理其他数据，不中断整个流程
                }
            }
        }
        log.info("getQuotationHistoryTrendList_allHistoryTrendList.size={}", allHistoryIndexTrendList.size());
        return allHistoryIndexTrendList;
    }

    private List<HistoryTrendIndexDTO> processStockDateData(String stockCode, String date, List<List<Number>> dataArrays) {
        List<HistoryTrendIndexDTO> historyIndexTrendList = new ArrayList<>();
        // 获取配置数组 - 最后一行
        List<Number> configArray = dataArrays.get(dataArrays.size() - 1);
        if (configArray == null || configArray.size() < 10) {
            throw new RuntimeException("配置数组格式不正确");
        }
        // 解析配置
        List<Integer> indicatorIds = new ArrayList<>();
        List<Integer> decimalShifts = new ArrayList<>();
        for (int i = 0; i < 5; i++) {
            indicatorIds.add(configArray.get(i).intValue());
            decimalShifts.add(configArray.get(i + 5).intValue());
        }
        Collections.reverse(decimalShifts);
        // 数据异常检测
        long sum = dataArrays.stream()
                .filter(subList -> subList != null && subList.size() > 1 && subList.get(1) != null)
                .mapToLong(subList -> subList.get(1).longValue())
                .sum();

        if (sum > 160000) {
            throw new RuntimeException("数据异常，sum: " + sum);
        }
        // 获取各指标的索引位置
        int timeIndex = indicatorIds.indexOf(SpeedIndicatorEnum.TRADE_TIME.getIndicator());
        //最新价
        int latestPriceIndex = indicatorIds.indexOf(SpeedIndicatorEnum.NEW_PRICE.getIndicator());
        //总成交额
        int totalAmount = indicatorIds.indexOf(SpeedIndicatorEnum.TOTAL_AMOUNT.getIndicator());
        //总成交量
        int totalVolume = indicatorIds.indexOf(SpeedIndicatorEnum.TOTAL_VOLUME.getIndicator());
        // 检查必要的索引是否存在
        if (timeIndex < 0 || latestPriceIndex < 0) {
            throw new RuntimeException("缺少必要的指标索引");
        }
        // 解析日期
        LocalDate localDate;
        try {
            localDate = LocalDate.parse(date, DateTimeFormatter.ofPattern("yyyyMMdd"));
        } catch (Exception e) {
            throw new RuntimeException("日期解析失败: " + date, e);
        }
        // 处理每一行数据（排除最后一行配置数据）
        int time_s = 0;
        double latestPrice = 0.0;
        for (int i = 0; i < dataArrays.size() - 1; i++) {
            List<Number> row = dataArrays.get(i);
            if (row == null || row.isEmpty()) {
                continue;
            }
            // 检查行数据完整性
            int maxIndex = Math.max(Math.max(timeIndex, latestPriceIndex),
                    Math.max(totalAmount >= 0 ? totalAmount : 0,
                            totalVolume >= 0 ? totalVolume : 0));
            if (row.size() <= maxIndex) {
                log.warn("行数据不完整，跳过该行: stockCode={}, date={}, rowIndex={}", stockCode, date, i);
                continue;
            }
            try {
                // 累加时间
                Number timeNum = row.get(timeIndex);
                if (timeNum != null) {
                    time_s += timeNum.intValue();
                }
                // 解析时间
                int hours = time_s / 10000;
                int minutes = (time_s % 10000) / 100;
                int seconds = time_s % 100;
                // 验证时间有效性
                if (hours < 0 || hours > 23 || minutes < 0 || minutes > 59 || seconds < 0 || seconds > 59) {
                    log.warn("时间数据异常，跳过该行: time_s={}", time_s);
                    continue;
                }
                LocalTime time = LocalTime.of(hours, minutes, seconds);
                LocalDateTime dateTime = LocalDateTime.of(localDate, time);

                // 创建DTO
                HistoryTrendIndexDTO historyTrendIndexDTO = new HistoryTrendIndexDTO();
                historyTrendIndexDTO.setTradeDate(dateTime);
                historyTrendIndexDTO.setWindCode(stockCode);

                // 设置价格数据（累加）
                Number latestPriceNum = row.get(latestPriceIndex);
                if (latestPriceNum != null) {
                    latestPrice += latestPriceNum.doubleValue();
                }
                historyTrendIndexDTO.setLatestPrice(latestPrice);

                // 设置成交额数据（不累加）
                if (totalAmount >= 0) {
                    Number averagePriceNum = row.get(totalAmount);
                    if (averagePriceNum != null) {
                        historyTrendIndexDTO.setTotalAmount(averagePriceNum.doubleValue());
                    } else {
                        historyTrendIndexDTO.setTotalAmount(0.0);
                    }
                } else {
                    historyTrendIndexDTO.setTotalAmount(0.0);
                }

                // 设置成交量数据（不累加）
                if (totalVolume >= 0) {
                    Number volumeNum = row.get(totalVolume);
                    if (volumeNum != null) {
                        historyTrendIndexDTO.setTotalVolume(volumeNum.doubleValue());
                    } else {
                        historyTrendIndexDTO.setTotalVolume(0.0);
                    }
                } else {
                    historyTrendIndexDTO.setTotalVolume(0.0);
                }
                historyIndexTrendList.add(historyTrendIndexDTO);

            } catch (Exception e) {
                log.error("处理行数据失败: stockCode={}, date={}, rowIndex={}, error={}",
                        stockCode, date, i, e.getMessage());
                // 继续处理下一行
            }
        }

        // 精度处理
        for (HistoryTrendIndexDTO historyTrendDTO : historyIndexTrendList) {
            try {
                // 处理最新价精度
                if (latestPriceIndex >= 0 && latestPriceIndex < decimalShifts.size()) {
                    historyTrendDTO.setLatestPrice(MathUtil.formatDecimal(
                            historyTrendDTO.getLatestPrice(), decimalShifts.get(latestPriceIndex), false));
                } else {
                    historyTrendDTO.setLatestPrice(MathUtil.formatDecimal(
                            historyTrendDTO.getLatestPrice(), 2, false));
                }
                // 处理成交额精度
                if (totalAmount >= 0 && totalAmount < decimalShifts.size()) {
                    historyTrendDTO.setTotalAmount(MathUtil.formatDecimal(
                            historyTrendDTO.getTotalAmount(), decimalShifts.get(totalAmount), false));
                } else {
                    historyTrendDTO.setTotalAmount(MathUtil.formatDecimal(
                            historyTrendDTO.getTotalAmount(), 2, false));
                }
                // 处理成交量精度
                historyTrendDTO.setTotalVolume(MathUtil.formatDecimal(
                        historyTrendDTO.getTotalVolume(), 2, false));

            } catch (Exception e) {
                log.error("精度处理失败: stockCode={}, date={}, error={}", stockCode, date, e.getMessage());
            }
        }
        return historyIndexTrendList;
    }


    /**
     * 根据时间区间获取A股历史分时数据
     *
     * @param startDate 起始日期
     * @param endDate   结束日期
     * @return 历史分时数据
     */
    @Override
    public List<HistoryTrendDTO> getHistoryTrendDataByDate(String startDate, String endDate) {
        if (!StringUtils.hasLength(endDate)) {
            endDate = DateUtil.getCurrentDateTimeByStr(DateTimeFormatConstants.COMPACT_DATE_FORMAT);
        }
        return quotationMapper.getHistoryTrendDataByDate(startDate, endDate);
    }
}
