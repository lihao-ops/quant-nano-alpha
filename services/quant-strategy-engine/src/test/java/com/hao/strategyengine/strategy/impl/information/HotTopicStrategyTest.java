package com.hao.strategyengine.strategy.impl.information;

import com.alibaba.fastjson.JSON;
import com.hao.strategyengine.common.model.core.StrategyContext;
import com.hao.strategyengine.common.model.response.StrategyResult;
import lombok.extern.slf4j.Slf4j;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * ✅ HotTopicStrategy 集成测试
 * 运行环境：Spring Boot 容器
 * 验证：
 * 1️⃣ Bean 是否能正常注入
 * 2️⃣ execute() 能否正确执行（基于 Mock Redis 数据）
 */
@Slf4j
@SpringBootTest
public class HotTopicStrategyTest {

    @Autowired
    private HotTopicStrategy hotTopicStrategy;

    @Test
    @DisplayName("1️⃣ 验证策略ID是否正确")
    void getId() {
        String id = hotTopicStrategy.getId();
        log.info("✅ 策略ID = {}", id);
    }

    @Test
    @DisplayName("2️⃣ 执行策略（按题材ID查询）")
    void execute_byTopicId() {
        // 构造 StrategyContext（携带 topicId）
        Map<String, Object> extra = new HashMap<>();
        extra.put("topicId", 298);
        StrategyContext context = StrategyContext.builder()
                .userId(001)
                .symbol("AAPL")
                .extra(extra)
                .build();

        StrategyResult result = hotTopicStrategy.execute(context);

        log.info("✅ 执行结果 = {}", JSON.toJSONString(result));

        // 验证结果
        assert result != null;
        assert !((List<?>) result.getData()).isEmpty();
    }
//
//    @Test
//    @DisplayName("3️⃣ 执行策略（按题材名称模糊查询）")
//    void execute_byTopicName() {
//        // 构造 StrategyContext（携带 topicName）
//        Map<String, Object> extra = new HashMap<>();
//        extra.put("topicName", "001");
//
//        StrategyContext context = StrategyContext.builder()
//                .userId("user002")
//                .symbol("TSLA")
//                .extra(extra)
//                .build();
//
//        StrategyResult result = hotTopicStrategy.execute(context);
//
//        log.info("✅ 模糊查询结果 = {}", JSON.toJSONString(result));
//
//        assert result != null;
//        assert !((List<?>) result.getData()).isEmpty();
//    }
}
