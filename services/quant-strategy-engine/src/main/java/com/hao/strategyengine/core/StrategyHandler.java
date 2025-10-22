package com.hao.strategyengine.core;

import com.hao.strategyengine.model.core.StrategyContext;

/**
 * StrategyHandler
 *
 * <p>策略处理器接口，用于对策略执行过程中的上下文进行处理。
 * 可用于责任链模式（Chain of Responsibility），在策略执行前后做各种前置或后置处理。</p>
 *
 * <p>典型使用场景：</p>
 * <ul>
 *     <li>风控校验（Risk Control）</li>
 *     <li>参数校验（Validation）</li>
 *     <li>限流（Rate Limiting）</li>
 *     <li>日志记录（Logging）</li>
 * </ul>
 *
 * <p>示例用法：</p>
 * <pre>{@code
 * public class LoggingHandler implements StrategyHandler {
 *     @Override
 *     public void handle(StrategyContext ctx) {
 *         log.info("策略执行上下文：{}", ctx);
 *     }
 * }
 * }</pre>
 *
 * @author hli
 * @date 2025-10-22
 */
public interface StrategyHandler {

    /**
     * 处理策略上下文
     *
     * @param ctx 策略执行上下文
     * @throws Exception 可抛出异常以中断策略执行链或反馈错误
     */
    void handle(StrategyContext ctx) throws Exception;
}
