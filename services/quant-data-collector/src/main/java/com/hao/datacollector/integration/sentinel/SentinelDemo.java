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

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ThreadLocalRandom;

/*
实现思路概述（Sentinel FlowRule + DegradeRule 极简演示）

背景：用 Sentinel 在单进程里为一个资源设置“限流”和“熔断降级”，并通过简单的随机慢调用/异常来触发观察效果。
资源定义：用字符串 RESOURCE 代表受控的接口/方法。
限流（FlowRule）：
维度：QPS（每秒请求数）。
阈值：count=10，超过后直接拒绝（默认快速失败）。
目的：在正常场景下控速，保护系统不被瞬时流量压垮。
熔断降级（DegradeRule）：
策略1：慢调用比例（RT）。当统计窗口内请求量达到 minRequestAmount，且“慢调用比例”超过 slowRatioThreshold，就熔断 timeWindow 秒。
慢调用的定义通过 count（RT阈值，毫秒）确定。
策略2：异常比例。异常占比超过阈值 count 时熔断。
目的：在下游变慢或错误率升高时，快速自我保护并降级。
运行流程：
启动时初始化并加载 FlowRule 与 DegradeRule。
主循环中用 SphU.entry(RESOURCE) 进入受控资源：
成功进入：执行“模拟业务逻辑”，用随机数制造 5% 异常、20% 慢调用，其余正常。
被拦截：抛 BlockException，说明被限流或熔断，走降级输出。
业务异常：用 Tracer.trace 上报，参与异常比例熔断统计。
观测点：
正常输出“ok”。
超过限流阈值或处于熔断状态时输出“blocked: xxx”。
业务异常输出“biz error: ...”，并可能触发异常比例熔断。
参数选择原则（示例值，需按业务微调）：
限流 QPS 选系统可承受的稳定值；RT慢阈值选关键依赖的SLA边界；minRequestAmount与statIntervalMs防止低流量误判；timeWindow不宜过长，便于恢复。
*/
public class SentinelDemo {

    private static final String RESOURCE = "demo:api";

    public static void main(String[] args) throws Exception {
        initFlowRules();
        initDegradeRules();

        // 简单模拟请求 200 次
        for (int i = 0; i < 200; i++) {
            try (Entry entry = SphU.entry(RESOURCE)) {
                // 业务逻辑
                // 随机制造慢请求或异常，用于观察熔断触发
                int r = ThreadLocalRandom.current().nextInt(100);
                if (r < 5) { // 5% 概率抛异常
                    throw new RuntimeException("mock error");
                }
                if (r < 20) { // 20% 概率变慢，sleep 300ms
                    Thread.sleep(300);
                } else {
                    Thread.sleep(50);
                }
                System.out.println("ok " + i);
            } catch (BlockException ex) {
                // 被限流或熔断时的降级逻辑
                System.out.println("blocked: " + ex.getClass().getSimpleName());
            } catch (Exception bizEx) {
                // 记录业务异常，便于异常比例熔断统计
                Tracer.trace(bizEx);
                System.out.println("biz error: " + bizEx.getMessage());
            }
        }
    }

    private static void initFlowRules() {
        StringBuilder sb = new StringBuilder();
        sb.deleteCharAt(sb.length() - 1);
        List<FlowRule> rules = new ArrayList<>();
        FlowRule rule = new FlowRule();
        rule.setResource(RESOURCE);
        rule.setGrade(RuleConstant.FLOW_GRADE_QPS); // 按 QPS 限流
        rule.setCount(10); // 阈值：每秒最多 10 次
        // 默认行为：超限直接拒绝（快速失败）
        rules.add(rule);
        FlowRuleManager.loadRules(rules);
    }

    private static void initDegradeRules() {
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