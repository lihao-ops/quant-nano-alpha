package com.hao.strategyengine.integration.feign;

import com.alibaba.fastjson.JSON;
import com.hao.strategyengine.common.model.vo.datacollector.StockBasicInfoQueryResultVO;
import lombok.extern.slf4j.Slf4j;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import java.util.List;

@Slf4j
@SpringBootTest
public class DataCollectorClientTest {

    @Autowired
    private DataCollectorClient client;

    @Test
    void getTradeDateListByTime() {
        List<String> dateList = client.getTradeDateListByTime("20240101", "20241212");
        log.info("dateList={}", JSON.toJSONString(dateList));
    }

    @Test
    void queryStockBasicInfo() {
        List<StockBasicInfoQueryResultVO> infoQueryResultVOS = client.queryStockBasicInfo("000001.SZ", null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null);
        log.info("infoQueryResultVOS={}", JSON.toJSONString(infoQueryResultVOS));
    }
}