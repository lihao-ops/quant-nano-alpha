package com.hao.riskcontrol.integration.feign;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;

@SpringBootTest
class QuotationClientTest {

    @Autowired
    private QuotationClient client;

    @Test
    void batchRealTimeQuotation() {
        List<String> windCodes = new ArrayList<>(Arrays.asList("002623.SZ", "600819.SH", "600519.SH", "000001.SZ"));
        List<Integer> indicatorIds = new ArrayList<>(Arrays.asList(3, 8, 81, 2, 198, 187, 205, 182, 171, 1256));
        Map<String, Map<Integer, Object>> result = client.batchRealTimeQuotation("b7cace14707a497dbe2d0250054f27f5", "2.0", windCodes, indicatorIds);
        System.out.println();
    }
}