package com.hao.strategyengine.chain;

import com.hao.strategyengine.common.model.core.StrategyContext;
import com.hao.strategyengine.core.StrategyHandler;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

/**
 * 风控检查处理器
 *
 * 设计目的：
 * 1. 在策略执行前进行风险拦截。
 * 2. 与责任链模式配合，形成可扩展的风控校验链路。
 *
 * 为什么需要该类：
 * - 风控属于前置保护，需要在执行链最前端校验。
 *
 * 核心实现思路：
 * - 从上下文extra读取riskBlocked标志，命中则抛异常。
 */
@Slf4j
@Component
public class RiskCheckHandler implements StrategyHandler {

    /**
     * 执行风控检查
     *
     * 实现逻辑：
     * 1. 读取上下文风险标志。
     * 2. 命中风险则抛出异常阻断执行。
     *
     * @param ctx 策略上下文
     * @throws Exception 风控命中时抛出异常
     */
    @Override
    public void handle(StrategyContext ctx) throws Exception {
        // 实现思路：
        // 1. 读取风险标志并决定是否拦截。
        log.info("风控检查|Risk_check,userId={}", ctx.getUserId());
        // 第一步：从上下文读取风险标志
        if (ctx.getExtra() != null && Boolean.TRUE.equals(ctx.getExtra().get("riskBlocked"))) {
            // 第二步：命中风险拦截，抛出异常中断策略执行
            throw new RuntimeException("风险拦截：userId=" + ctx.getUserId());
        }
        // 第三步：未命中则放行到责任链下一个处理器
    }
}
