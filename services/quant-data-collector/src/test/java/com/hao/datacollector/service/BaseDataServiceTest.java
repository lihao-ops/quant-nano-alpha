package com.hao.datacollector.service;

import com.hao.datacollector.dto.param.base.CloudDataParams;
import com.hao.datacollector.dto.param.base.StockInfoDailyDTO;
import lombok.extern.slf4j.Slf4j;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import java.util.ArrayList;
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
@Slf4j
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
     * 测试获取云数据并批量入库
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
        
        // 1. 获取云数据
        List<List<Object>> result = baseDataService.getCloudData(params);
        log.info("获取云数据结果|Get_cloud_data_result,size={}", result.size());

        // 2. 转换为 DTO 列表
        List<StockInfoDailyDTO> stockList = new ArrayList<>();
        String tradeDate = "20251225";
        
        if (result != null && !result.isEmpty()) {
            for (List<Object> row : result) {
                if (row.size() >= 2) { // 确保有足够的列: code, name, date
                    StockInfoDailyDTO dto = new StockInfoDailyDTO();
                    dto.setWindCode(String.valueOf(row.get(0)));
                    dto.setWindName(String.valueOf(row.get(1)));
                    stockList.add(dto);
                }
            }
        }

        // 3. 批量入库
        if (!stockList.isEmpty() && tradeDate != null) {
            // 格式化日期，假设返回的是 yyyyMMdd，需要转为 yyyy-MM-dd 或者直接使用（取决于数据库字段类型）
            // 这里假设数据库是 DATE 类型，且 Mapper 中直接使用了字符串，可能需要根据实际情况调整格式
            // 如果 tradeDate 是 "20250621"，可能需要转为 "2025-06-21"
            // 这里简单处理，直接传入
            Boolean b = baseDataService.batchInsertStockInfoDaily(stockList, tradeDate);
            log.info("批量入库结果|Batch_insert_result,success={}", b);
        } else {
            log.warn("无数据需要入库|No_data_to_insert");
        }
    }
}
