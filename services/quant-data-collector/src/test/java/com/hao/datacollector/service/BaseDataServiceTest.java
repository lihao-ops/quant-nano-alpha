package com.hao.datacollector.service;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

@SpringBootTest
class BaseDataServiceTest {
    @Autowired
    private BaseDataService baseDataService;

//    @Test
//    void batchInsertStockBasicInfo() {
//
//        File file = new File("C:\\Users\\lihao\\Desktop\\data\\基础信息.xlsx");
//        baseDataService.batchInsertStockBasicInfo(file);
//    }

//    @Test
//    void batchInsertStockDailyMetricsDTO() {
//        baseDataService.getInsertStockMarketData("002366.SZ", "2025-05-28", "2025-05-28");
//    }

    @Test
    void batchInsertStockMarketData() {
        baseDataService.batchInsertStockMarketData("2025-01-01", "2025-06-06");
    }
}