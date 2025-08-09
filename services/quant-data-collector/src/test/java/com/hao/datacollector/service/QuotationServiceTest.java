package com.hao.datacollector.service;

import com.hao.datacollector.common.cache.DateCache;
import com.hao.datacollector.common.cache.StockCache;
import com.hao.datacollector.common.constant.DateTimeFormatConstants;
import com.hao.datacollector.common.utils.DateUtil;
import com.hao.datacollector.dal.dao.QuotationMapper;
import lombok.extern.slf4j.Slf4j;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

@Slf4j
@SpringBootTest
class QuotationServiceTest {

    @Autowired
    private QuotationService quotationService;
    @Autowired
    private QuotationMapper quotationMapper;

    @Test
    void transferQuotationBaseByStock() {
        List<String> allWindCodeList = new ArrayList<>(StockCache.allWindCode);
//        String startDate = DateUtil.formatLocalDate(DateCache.CurrentYearTradeDateList.get(0), DateTimeFormatConstant.EIGHT_DIGIT_DATE_FORMAT);
        String startDate = "20250718";
        String endDate = DateUtil.stringTimeToAdjust(DateUtil.getCurrentDateTimeByStr(DateTimeFormatConstants.EIGHT_DIGIT_DATE_FORMAT), DateTimeFormatConstants.EIGHT_DIGIT_DATE_FORMAT, 1);
        //需要剔除已经转档的股票
        List<String> endWindCodeList = quotationMapper.getJobQuotationBaseEndWindCodeList(startDate, endDate);
        allWindCodeList.removeAll(endWindCodeList);
        for (String windCode : allWindCodeList) {
            Boolean transferResult = quotationService.transferQuotationBaseByStock(windCode, startDate, endDate);
            log.info("transferQuotationBaseByStock_result={},windCode={},startDate={},endDate={}", transferResult, windCode, startDate, endDate);
        }
    }

    @Test
    void transferQuotationHistoryTrend() {
        List<String> allWindCodeList = new ArrayList<>(StockCache.allWindCode);
        List<String> yearTradeDateList = DateUtil.formatLocalDateList(DateCache.Year2023TradeDateList, DateTimeFormatConstants.EIGHT_DIGIT_DATE_FORMAT);
        //从当年已转档的最大日期(包含),并且剔除最大日期已经转档过的windCode,继续开始转档
        String maxEndDate = quotationMapper.getMaxHistoryTrendEndDate("2023");
//        String maxEndDate = "20231009";
        List<String> completedWindCodes = quotationMapper.getCompletedWindCodes(maxEndDate);
        int tradeDateIndexOf = yearTradeDateList.indexOf(maxEndDate);
        int batchSize = 100;
        if (tradeDateIndexOf != -1) {
            List<String> needFillBack = allWindCodeList.stream()
                    .filter(code -> !completedWindCodes.contains(code))
                    .collect(Collectors.toList());
            if (!needFillBack.isEmpty()) {
                log.info("补偿转档 {} 日期未完成的 {} 个 windCode", maxEndDate, needFillBack.size());
                List<String> needFillBackTradeDateList = new ArrayList<>();
                needFillBackTradeDateList.add(maxEndDate);
                transferOneDay(needFillBackTradeDateList, needFillBack, batchSize);
            }
            // 正常转档后续日期
            yearTradeDateList = new ArrayList<>(yearTradeDateList.subList(tradeDateIndexOf + 1, yearTradeDateList.size()));
            transferOneDay(yearTradeDateList, allWindCodeList, batchSize);
        }
    }

    private void transferOneDay(List<String> yearTradeDateList, List<String> windCodes, int batchSize) {
        int totalSize = windCodes.size();
        for (String tradeDate : yearTradeDateList) {
            if (tradeDate.contains("202311")) {
                throw new RuntimeException("202311!!!!");
            }
            for (int i = 0; i < totalSize; i += batchSize) {
                List<String> subList = windCodes.subList(i, Math.min(i + batchSize, totalSize));
                String windCodeStr = String.join(",", subList);
                Boolean transferResult = quotationService.transferQuotationHistoryTrend(Integer.parseInt(tradeDate), windCodeStr, 0);
                log.info("transferQuotationHistoryTrend_result={}, tradeDate={}, windCodes={}", transferResult, tradeDate, windCodeStr);
            }
        }
    }
}