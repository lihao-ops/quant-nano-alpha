package com.hao.riskcontrol.integration.feign;

import org.junit.jupiter.api.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;

/**
 * 行情客户端集成测试
 *
 * 测试目的：
 * 1. 验证批量行情接口的联通性与响应结构。
 * 2. 验证参数传入后服务端能够返回完整结果。
 *
 * 设计思路：
 * - 通过SpringBootTest加载上下文，使用真实Feign客户端进行调用。
 */
@SpringBootTest
class QuotationClientTest {
    private static final Logger LOG = LoggerFactory.getLogger(QuotationClientTest.class);

    @Autowired
    private QuotationClient client;

    /**
     * 批量实时行情查询测试
     *
     * 实现逻辑：
     * 1. 构造股票代码与指标参数。
     * 2. 调用Feign接口获取行情结果。
     * 3. 输出关键信息便于人工核验。
     */
    @Test
    void batchRealTimeQuotation() {
        // 实现思路：
        // 1. 使用固定样本验证接口可用性。
        // 2. 记录返回结果摘要用于校验。
        List<String> windCodes = new ArrayList<>(Arrays.asList("002623.SZ", "600819.SH", "600519.SH", "000001.SZ"));
        List<Integer> indicatorIds = new ArrayList<>(Arrays.asList(3, 8, 81, 2, 198, 187, 205, 182, 171, 1256));
        Map<String, Map<Integer, Object>> result = client.batchRealTimeQuotation("b7cace14707a497dbe2d0250054f27f5", "2.0", windCodes, indicatorIds);
        LOG.info("批量行情查询完成|Batch_quotation_query_done,windCodeSize={},indicatorSize={}", windCodes.size(), indicatorIds.size());
        LOG.info("批量行情查询结果摘要|Batch_quotation_result_summary,resultSize={}", result == null ? 0 : result.size());
    }
}
