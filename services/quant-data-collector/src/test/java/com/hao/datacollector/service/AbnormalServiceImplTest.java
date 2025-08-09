package com.hao.datacollector.service;


import com.alibaba.fastjson.JSON;
import com.hao.datacollector.dto.param.abnormal.IndexSourceParam;
import com.hao.datacollector.web.vo.abnormal.ActiveRankRecordVO;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import java.util.List;

@SpringBootTest
class AbnormalServiceImplTest {

    @Autowired
    private AbnormalService abnormalService;


    @Test
    public void getHomePage() {
//        IndexSourceParam indexSourceParam = new IndexSourceParam();
//        indexSourceParam.setTradeDate("20250620");
//        Boolean b = abnormalService.transferHomePage(indexSourceParam);


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