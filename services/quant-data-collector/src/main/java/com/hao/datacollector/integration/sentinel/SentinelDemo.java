package com.hao.datacollector.integration.sentinel;

import com.alibaba.csp.sentinel.Entry;
import com.alibaba.csp.sentinel.SphU;
import com.alibaba.csp.sentinel.Tracer;
import com.alibaba.csp.sentinel.slots.block.BlockException;
import com.alibaba.csp.sentinel.slots.block.RuleConstant;
import com.alibaba.csp.sentinel.slots.block.degrade.DegradeRule;
import com.alibaba.csp.sentinel.slots.block.degrade.DegradeRuleManager;
import com.alibaba.csp.sentinel.slots.block.flow.FlowRule;
import com.alibaba.csp.sentinel.slots.block.flow.FlowRuleManager;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ThreadLocalRandom;

/**
 * Sentinel限流与熔断演示
 *
 * 职责：演示单进程内为资源配置限流与熔断规则，并输出触发效果。
 *
 * 设计目的：
 * 1. 说明FlowRule与DegradeRule的组合使用方式。
 * 2. 展示慢调用与异常比例触发效果。
 *
 * 为什么需要该类：
 * - 作为限流与熔断规则的可运行示例，便于验证配置。
 *
 * 核心实现思路：
 * - 初始化限流与熔断规则，循环进入受控资源并模拟慢调用/异常。
 */
public class SentinelDemo {
    private static final Logger LOG = LoggerFactory.getLogger(SentinelDemo.class);

    private static final String RESOURCE = "demo:api";

    /**
     * 演示入口
     *
     * 实现逻辑：
     * 1. 初始化限流与熔断规则。
     * 2. 循环访问资源并模拟慢调用与异常。
     *
     * @param args 启动参数
     * @throws Exception 异常
     */
    public static void main(String[] args) throws Exception {
        // 实现思路：规则初始化后执行受控请求循环
        initFlowRules();
        initDegradeRules();

        // 简单模拟请求 200 次
        for (int i = 0; i < 200; i++) {
            try (Entry entry = SphU.entry(RESOURCE)) {
                // 业务逻辑
                // 随机制造慢请求或异常，用于观察熔断触发
                int r = ThreadLocalRandom.current().nextInt(100);
                if (r < 5) { // 5% 概率抛异常
                    throw new RuntimeException("mock_error");
                }
                if (r < 20) { // 20% 概率变慢，sleep 300ms
                    Thread.sleep(300);
                } else {
                    Thread.sleep(50);
                }
                LOG.info("请求通过|Request_pass,index={}", i);
            } catch (BlockException ex) {
                // 被限流或熔断时的降级逻辑
                LOG.warn("请求被拦截|Request_blocked,reason={}", ex.getClass().getSimpleName());
            } catch (Exception bizEx) {
                // 记录业务异常，便于异常比例熔断统计
                Tracer.trace(bizEx);
                LOG.error("业务异常|Business_error,error={}", bizEx.getMessage(), bizEx);
            }
        }
    }

    /**
     * 初始化限流规则
     *
     * 实现逻辑：
     * 1. 构建QPS限流规则。
     * 2. 加载至规则管理器。
     */
    private static void initFlowRules() {
        // 实现思路：设置资源QPS阈值并加载
        List<FlowRule> rules = new ArrayList<>();
        FlowRule rule = new FlowRule();
        rule.setResource(RESOURCE);
        rule.setGrade(RuleConstant.FLOW_GRADE_QPS); // 按 QPS 限流
        rule.setCount(10); // 阈值：每秒最多 10 次
        // 默认行为：超限直接拒绝（快速失败）
        rules.add(rule);
        FlowRuleManager.loadRules(rules);
    }

    /**
     * 初始化熔断规则
     *
     * 实现逻辑：
     * 1. 配置慢调用比例熔断。
     * 2. 配置异常比例熔断。
     */
    private static void initDegradeRules() {
        // 实现思路：配置RT与异常比例熔断规则
        List<DegradeRule> rules = new ArrayList<>();

        // 策略一：慢调用比例熔断（RT）
        DegradeRule rtRule = new DegradeRule();
        rtRule.setResource(RESOURCE);
        rtRule.setGrade(RuleConstant.DEGRADE_GRADE_RT);
        rtRule.setCount(200);           // 慢调用阈值：超过 200ms 视为慢
        rtRule.setTimeWindow(5);        // 熔断持续 5 秒
        rtRule.setMinRequestAmount(20); // 至少 20 个请求后才评估
        rtRule.setStatIntervalMs(1000); // 统计窗口 1 秒
        rtRule.setSlowRatioThreshold(0.5); // 慢调用比例超过 50% 触发熔断
        rules.add(rtRule);

        // 策略二：异常比例熔断
        DegradeRule errRule = new DegradeRule();
        errRule.setResource(RESOURCE);
        errRule.setGrade(RuleConstant.DEGRADE_GRADE_EXCEPTION_RATIO);
        errRule.setCount(0.3);          // 异常比例阈值 30%
        errRule.setTimeWindow(5);       // 熔断 5 秒
        errRule.setMinRequestAmount(20);
        errRule.setStatIntervalMs(1000);
        rules.add(errRule);

        DegradeRuleManager.loadRules(rules);
    }
}
