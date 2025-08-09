package com.hao.datacollector.service.impl;

import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.TypeReference;
import com.hao.datacollector.common.constant.DataSourceConstants;
import com.hao.datacollector.common.constant.DateTimeFormatConstants;
import com.hao.datacollector.common.utils.DateUtil;
import com.hao.datacollector.common.utils.HttpUtil;
import com.hao.datacollector.common.utils.MathUtil;
import com.hao.datacollector.dal.dao.QuotationMapper;
import com.hao.datacollector.dto.quotation.HistoryTrendDTO;
import com.hao.datacollector.dto.table.quotation.QuotationStockBaseDTO;
import com.hao.datacollector.service.QuotationService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpHeaders;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;

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
    @Value("${wind_base.session_id}")
    private String windSessionId;

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
        headers.add(DataSourceConstants.WIND_POINT_SESSION_NAME, windSessionId);
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
     * 获取股票历史分时对象List
     *
     * @param tradeDate 交易日期,如:20220608
     * @param windCodes 股票代码List
     * @param dateType  时间类型,0表示固定时间
     * @return 操作结果
     */
    private List<HistoryTrendDTO> getQuotationHistoryTrendList(int tradeDate, String windCodes, Integer dateType) {
        HttpHeaders headers = new HttpHeaders();
        headers.add(DataSourceConstants.WIND_POINT_SESSION_NAME, windSessionId);
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
}
