package com.hao.datacollector.service;

import com.hao.datacollector.dto.param.base.CloudDataParams;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * 基础数据服务测试类
 *
 * 测试目的：
 * 1. 验证基础行情数据批量入库流程是否可用。
 * 2. 验证指定日期区间的数据拉取与入库是否稳定。
 *
 * 设计思路：
 * - 通过Spring上下文加载真实依赖。
 * - 使用固定日期区间回放核心流程。
 */
@SpringBootTest
class BaseDataServiceTest {
    @Autowired
    private BaseDataService baseDataService;

//    @Test
//    void batchInsertStockBasicInfo() {
//
//        File file = new File("data/基础信息.xlsx");
//        baseDataService.batchInsertStockBasicInfo(file);
//    }

//    @Test
//    void batchInsertStockDailyMetricsDTO() {
//        baseDataService.getInsertStockMarketData("002366.SZ", "2025-05-28", "2025-05-28");
//    }

    /**
     * 批量拉取并入库股票行情数据
     *
     * 实现逻辑：
     * 1. 传入日期区间触发行情数据拉取。
     * 2. 验证批处理入库流程是否可执行。
     */
    @Test
    void batchInsertStockMarketData() {
        // 实现思路：
        // 1. 使用固定日期范围验证完整链路。
        // 2. 由服务内部处理分段拉取与入库。
        baseDataService.batchInsertStockMarketData("2025-01-01", "2025-06-06");
    }

    /**
     * 测试获取云数据
     */
    @Test
    void getCloudData() {
        CloudDataParams params = new CloudDataParams();
        params.setCommand("DEV_COMMON_REPORT");
        params.setCloudType(0);
        Map<String, String> cloudParams = new HashMap<>();
        cloudParams.put("reportBody", "WSS('macro=a001010100000000','s_info_name','tradeDate=s_trade_date(windcode,now(), 0)')");
        params.setCloudParams(cloudParams);
        params.setLan("zh");
        // sessionId 可以不传，测试 Service 内部自动填充逻辑
        List<List<Object>> result = baseDataService.getCloudData(params);
        System.out.println("Cloud Data Result: " + result);
    }
}
