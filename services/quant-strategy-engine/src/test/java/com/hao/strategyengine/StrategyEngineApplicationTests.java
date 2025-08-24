package com.hao.strategyengine;

import com.hao.strategyengine.integration.feign.DataCollectorClient;
import com.hao.strategyengine.web.vo.datacollector.StockBasicInfoQueryResultVO;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import java.util.List;

@SpringBootTest
class StrategyEngineApplicationTests {

    @Autowired
    private DataCollectorClient client;

    @Test
    void contextLoads() {
        List<StockBasicInfoQueryResultVO> stockBasicInfoQueryResultVOS = client.queryStockBasicInfo("000001.SZ", null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null);
        List<String> tradeDateListByTime = client.getTradeDateListByTime("20240101", "20241212");
    }
}
