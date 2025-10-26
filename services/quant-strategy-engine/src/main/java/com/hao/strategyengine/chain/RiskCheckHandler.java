package com.hao.strategyengine.chain;

import com.hao.strategyengine.common.model.core.StrategyContext;
import com.hao.strategyengine.core.StrategyHandler;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

/**
 * ===============================================================
 * 【类名】：RiskCheckHandler（风险控制责任节点）
 * ===============================================================
 * <p>
 * 【功能定位】：
 * ⦿ 本类是策略执行前的第一道“风控过滤器”；
 * ⦿ 用于检查用户或请求是否被风险系统标记为不可执行；
 * ⦿ 属于责任链（Chain of Responsibility）模式中的一个处理节点。
 * <p>
 * 【设计模式】：
 * ➤ 责任链模式（Chain Pattern）
 * - 每个 Handler 独立处理一个环节；
 * - 可按顺序串联多个 Handler；
 * - 当当前 Handler 通过时，责任链继续传递；
 * - 若校验失败，抛出异常中断执行。
 * <p>
 * 【核心逻辑】：
 * - 从 StrategyContext 的 extra 参数中读取风险标志；
 * - 若存在 "riskBlocked=true"，则拒绝执行；
 * - 抛出 RuntimeException 中断策略链。
 * <p>
 * 【执行链位置】：
 * ✅ 属于系统执行链的「第 1 步」（风控前置校验）：
 * Controller → Facade → Chain(⚙RiskCheckHandler) → Lock → Dispatcher → Strategy
 * <p>
 * 【执行流程】：
 * ┌──────────────────────────────────────────┐
 * │ Step 1：读取上下文中的风险标志（ctx.extra）     │
 * │ Step 2：判断是否包含 riskBlocked = true        │
 * │ Step 3：若命中 → 抛出异常并阻断后续执行         │
 * │ Step 4：若未命中 → 放行至下一个 Handler         │
 * └──────────────────────────────────────────┘
 * <p>
 * 【扩展建议】：
 * - 可与风控中心（如 Sentinel / 风控服务）对接；
 * - 可扩展为多级检查，如账户冻结、IP黑名单、交易时段校验等；
 * - 可在 StrategyChain 中灵活配置执行顺序。
 */
@Slf4j
@Component
public class RiskCheckHandler implements StrategyHandler {

    /**
     * ===============================================================
     * 【方法名】：handle
     * ===============================================================
     * <p>
     * 【功能】：
     * 执行风控检查逻辑，若检测到风险则直接中断策略执行流程。
     * <p>
     * 【参数】：
     *
     * @param ctx 策略上下文（包含 userId、symbol、extra 等信息）
     *            <p>
     *            【异常】：
     *            抛出 RuntimeException 表示风险命中，后续 Handler 不再执行。
     *            <p>
     *            【使用位置】：
     *            StrategyChain.apply(ctx) 调用链中的第一个 Handler。
     */
    @Override
    public void handle(StrategyContext ctx) throws Exception {
        log.info("风控检查：userId=" + ctx.getUserId());
        // Step 1️⃣ 从上下文的 extra 信息中读取“风险标志”
        if (ctx.getExtra() != null && Boolean.TRUE.equals(ctx.getExtra().get("riskBlocked"))) {
            // Step 2️⃣ 命中风险拦截，抛出异常中断策略执行
            throw new RuntimeException("风险拦截：userId=" + ctx.getUserId());
        }

        // Step 3️⃣ 否则放行（责任链继续传递给下一个 Handler）
    }
}
