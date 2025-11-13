package com.hao.strategyengine.chain;

/**
 * 类说明 / Class Description:
 * 中文：策略前置责任链，按顺序执行多个处理器节点（风控/校验/限流等）以保障策略执行安全与合规。
 * English: Pre-execution strategy responsibility chain; executes multiple handler nodes (risk/validation/rate-limit) in order to ensure safe and compliant strategy execution.
 *
 * 使用场景 / Use Cases:
 * 中文：在策略实际计算前统一进行前置检查与拦截。
 * English: Perform unified pre-checks and interceptions before actual strategy computation.
 *
 * 设计目的 / Design Purpose:
 * 中文：通过责任链模式解耦各类前置逻辑，保证可插拔、可扩展与顺序可控。
 * English: Decouple pre-logic via chain-of-responsibility, ensuring pluggability, extensibility, and ordered control.
 */
import com.hao.strategyengine.core.StrategyHandler;
import com.hao.strategyengine.common.model.core.StrategyContext;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import java.util.List;
/**
 * 运行流程联动（配合上层 Facade）
 * Controller
 *    ↓
 * Facade.executeAll()
 *    ↓
 * 【Step 1】StrategyChain.apply(ctx)      ← 当前类负责风控责任链
 *    ↓
 * 【Step 2】DistributedLockService.acquireOrWait()
 *    ↓
 * 【Step 3】StrategyDispatcher.dispatch()
 *    ↓
 * 【Step 4】QuantStrategy 执行具体策略
 */

/**
 * ===============================================================
 * 【类名】：StrategyChain（策略前置责任链）
 * ===============================================================
 *
 * 【功能定位】：
 *   ⦿ 该类负责组织并执行一组 StrategyHandler（处理节点），
 *      实现对策略执行的前置处理逻辑（如风控、验证、限流等）。
 *   ⦿ 是责任链（Chain of Responsibility）模式的核心调度类。
 *
 * 【核心思路】：
 *   - 系统中可能存在多个不同的 Handler（风控、白名单、账户冻结校验等）；
 *   - Spring 启动时自动注入所有实现 StrategyHandler 接口的 Bean；
 *   - StrategyChain 统一按顺序遍历执行；
 *   - 任意一个 Handler 抛出异常即可中断整个策略执行流程。
 *
 * 【执行链位置】：
 *   ✅ 属于系统执行链的「第 1 阶段：风控 / 前置校验阶段」
 *   Controller → Facade → ⚙ StrategyChain → Lock → Dispatcher → Strategy
 *
 * 【执行流程】：
 *   ┌────────────────────────────────────────┐
 *   │ Step 1：按顺序遍历所有注册的 Handler 实例      │
 *   │ Step 2：逐个执行 handle(ctx) 方法              │
 *   │ Step 3：若某个 Handler 抛异常 → 中断流程        │
 *   │ Step 4：所有 Handler 执行通过 → 放行策略阶段     │
 *   └────────────────────────────────────────┘
 *
 * 【设计模式】：
 *   ➤ Chain of Responsibility
 *     - 每个 Handler 专注于单一逻辑；
 *     - 顺序可控、可插拔；
 *     - 易于扩展和维护；
 *     - 常用于安全校验 / 请求过滤 / 风控前置逻辑。
 *
 * 【扩展方向】：
 *   - 可通过 @Order 注解控制 Handler 执行顺序；
 *   - 可引入动态配置（从数据库 / Nacos 加载启停策略）；
 *   - 可加入后置责任链（Post-Chain）用于审计或结果校验。
 */
@Component
@RequiredArgsConstructor
public class StrategyChain {

    /**
     * 系统自动注入的所有责任节点：
     *   - 由 Spring 扫描所有实现 StrategyHandler 的 Bean；
     *   - 以 List 形式注入；
     *   - 可通过 @Order 控制执行顺序。
     *
     * 示例：
     *   RiskCheckHandler   → 风控拦截
     *   LimitCheckHandler  → 限流检查
     *   AuthCheckHandler   → 鉴权验证
     */
    private final List<StrategyHandler> handlers;

    /**
     * ===============================================================
     * 【方法名】：apply
     * ===============================================================
     *
     * 【功能】：
     *   执行整个责任链的处理逻辑；
     *   若任一 Handler 抛出异常，则中止策略执行。
     *
     * 【参数】：
     *   @param ctx 策略上下文对象（StrategyContext）
     *
     * 【异常】：
     *   抛出 Exception 代表校验未通过，阻止后续执行。
     *
     * 【调用位置】：
     *   StrategyEngineFacade.executeAll() → Step 1 调用。
     */
    /**
     * 方法说明 / Method Description:
     * 中文：执行责任链中的所有前置处理器，如遇异常则中断策略执行。
     * English: Execute all pre-handlers in the chain; interrupt strategy execution on exception.
     *
     * 参数 / Parameters:
     * @param ctx 中文：策略上下文 / English: strategy context
     *
     * 返回值 / Return:
     * 中文：无返回值；通过异常控制流程中断 / English: void; flow interruption via exception
     *
     * 异常 / Exceptions:
     * 中文：处理器抛出异常表示校验未通过 / English: handler exceptions indicate validation failure
     */
    public void apply(StrategyContext ctx) throws Exception {
        // Step 1️⃣ 依次执行所有前置 Handler
        for (StrategyHandler handler : handlers) {
            // 中文：执行单一处理器的前置校验或风控逻辑
            // English: Execute pre-check or risk control logic of a single handler
            handler.handle(ctx);

        }

        // Step 2️⃣ 所有 Handler 校验通过，放行策略计算阶段
    }
}
