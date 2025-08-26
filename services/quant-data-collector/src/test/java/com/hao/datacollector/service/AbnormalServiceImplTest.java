package com.hao.datacollector.service;


import com.hao.datacollector.cache.DateCache;
import com.hao.datacollector.dto.param.abnormal.IndexSourceParam;
import constants.DateTimeFormatConstants;
import lombok.extern.slf4j.Slf4j;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import util.DateUtil;

import java.util.List;

@Slf4j
@SpringBootTest
class AbnormalServiceImplTest {

    @Autowired
    private AbnormalService abnormalService;


    @Test
    public void getHomePage() {
        //2021，2024已转，20250826(未完整)
        List<String> tradeDateList = DateUtil.formatLocalDateList(DateCache.CurrentYearTradeDateList, DateTimeFormatConstants.EIGHT_DIGIT_DATE_FORMAT);
        for (String tradeDate : tradeDateList) {
            IndexSourceParam indexSourceParam = new IndexSourceParam();
            indexSourceParam.setTradeDate(tradeDate);
            Boolean result = false;
//            if (tradeDate.equals("20200111"。20220127,20230810)){
//                continue;
//            }
            try {
                result = abnormalService.transferHomePage(indexSourceParam);
            } catch (Exception e) {
                result = abnormalService.transferHomePage(indexSourceParam);
            }
            log.info("getHomePage_tradeDate={},result={}", tradeDate, result);
        }


        Boolean sourceListOfSeats = abnormalService.transferListOfSeats(3, 0, 1, 10000, 1, -1);
//
//
//        System.out.println(JSON.toJSONString(sourceListOfSeats));
//        List<ActiveRankRecordVO> sourceActiveRank = abnormalService.getSourceActiveRank(3, 0, 10000, 1, -1);
//
//
//        System.out.println(JSON.toJSONString(sourceActiveRank));

        Boolean result = abnormalService.transferActiveRank(3, 0, 10000, 1, -1);
    }
}