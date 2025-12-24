package com.hao.strategyengine.strategy.impl.composite;

import com.hao.strategyengine.common.model.core.StrategyContext;
import com.hao.strategyengine.common.model.response.StrategyResult;
import com.hao.strategyengine.strategy.QuantStrategy;
import lombok.extern.slf4j.Slf4j;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * 复合策略 (Composite Strategy)
 *
 * <p><b>类职责:</b></p>
 * <p>实现组合模式，将多个独立的策略（子策略）聚合成一个单一的、可执行的复合策略单元。
 * 调用者可以像对待单个策略一样对待这个复合策略。</p>
 *
 * <p><b>设计目的:</b></p>
 * <ol>
 *     <li><b>简化调用:</b> 将一组强相关的策略打包，调用者只需执行一次，无需关心内部的多个子策略。</li>
 *     <li><b>逻辑复用:</b> 便于将常用的策略组合（如“动量+均值回归”）固化下来，在不同场景下复用。</li>
 *     <li><b>结构化:</b> 使策略的组织结构从扁平列表变为树形结构，更清晰地表达策略之间的层次和依赖关系。</li>
 * </ol>
 *
 * <p><b>为什么需要该类:</b></p>
 * <p>在复杂的量化模型中，通常需要多个因子或信号共同决策。复合策略模式提供了一种优雅的方式来组织和执行这些多阶段、多维度的策略逻辑，
 * 避免了在业务代码中出现冗长的if-else或串行调用，提升了代码的可读性和可维护性。</p>
 *
 * <p><b>核心实现思路:</b></p>
 * <ol>
 *     <li><b>树形结构:</b> 内部维护一个 {@code List<QuantStrategy>}，用于存储子策略节点。</li>
 *     <li><b>统一接口:</b> 实现 {@link QuantStrategy} 接口，使自身也能被其他复合策略包含，形成递归结构。</li>
 *     <li><b>委托执行:</b> 当 {@code execute} 方法被调用时，它会遍历内部的子策略列表，并依次执行它们。</li>
 *     <li><b>结果聚合:</b> 将每个子策略的执行结果 ({@link StrategyResult}) 收集起来，聚合成一个新的列表，作为复合策略自身的执行结果数据。</li>
 * </ol>
 */
@Slf4j
public class CompositeStrategy implements QuantStrategy {
    private final String id;
    private final List<QuantStrategy> children = new ArrayList<>();

    public CompositeStrategy(String id) {
        this.id = id;
    }

    /**
     * 向复合策略中添加一个子策略
     * @param strategy 待添加的子策略
     */
    public void add(QuantStrategy strategy) {
        children.add(strategy);
    }

    @Override
    public String getId() {
        return id;
    }

    /**
     * 执行复合策略
     *
     * <p><b>实现逻辑:</b></p>
     * <ol>
     *     <li>遍历所有子策略。</li>
     *     <li>依次执行每个子策略，并记录其开始和结束时间。</li>
     *     <li>将每个子策略的完整 {@link StrategyResult} 添加到结果列表中。</li>
     *     <li>如果任何子策略执行失败，则立即中断并返回失败结果。</li>
     *     <li>所有子策略成功后，构建一个包含所有子策略结果的、新的成功 {@link StrategyResult}。</li>
     * </ol>
     *
     * @param context 策略执行上下文
     * @return 聚合了所有子策略结果的复合策略结果
     */
    @Override
    public StrategyResult execute(StrategyContext context) {
        long start = System.currentTimeMillis();
        log.info("开始执行复合策略|Execute_composite_strategy_start,id={}", id);

        List<StrategyResult> childResults = new ArrayList<>();
        try {
            for (QuantStrategy childStrategy : children) {
                log.debug("开始执行子策略|Execute_child_strategy_start,compositeId={},childId={}", id, childStrategy.getId());
                StrategyResult childResult = childStrategy.execute(context);
                childResults.add(childResult);

                // 如果任何一个子策略失败，则整个复合策略失败
                if (!childResult.isSuccess()) {
                    log.error("子策略执行失败_复合策略中断|Child_strategy_failed_composite_aborted,compositeId={},failedChildId={}",
                            id, childStrategy.getId());
                    return buildErrorResult(start, "Child strategy " + childStrategy.getId() + " failed.");
                }
                log.debug("子策略执行成功|Execute_child_strategy_success,compositeId={},childId={}", id, childStrategy.getId());
            }

            log.info("复合策略执行成功|Composite_strategy_execution_success,id={},childCount={}", id, children.size());
            return StrategyResult.builder()
                    .strategyId(id)
                    .data(childResults) // 聚合完整的子策略结果
                    .durationMs(System.currentTimeMillis() - start)
                    .build();

        } catch (Exception e) {
            log.error("复合策略执行时发生未知异常|Unknown_exception_in_composite_strategy,id={}", id, e);
            return buildErrorResult(start, "Composite strategy execution failed with an unexpected error.");
        }
    }

    /**
     * 构建错误的策略结果
     * @param start 策略开始时间
     * @param errorMsg 错误信息
     * @return 包含错误信息的策略结果
     */
    private StrategyResult buildErrorResult(long start, String errorMsg) {
        Map<String, Object> errorData = new HashMap<>();
        errorData.put("error_message", errorMsg);
        return StrategyResult.builder()
                .strategyId(getId())
                .data(Collections.singletonList(errorData))
                .isSuccess(false)
                .durationMs(System.currentTimeMillis() - start)
                .build();
    }
}
