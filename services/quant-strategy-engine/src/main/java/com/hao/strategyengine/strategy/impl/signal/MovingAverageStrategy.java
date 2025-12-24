package com.hao.strategyengine.strategy.impl.signal;

import com.hao.strategyengine.common.model.core.StrategyContext;
import com.hao.strategyengine.common.model.response.StrategyResult;
import com.hao.strategyengine.strategy.QuantStrategy;
import enums.strategy.StrategyMetaEnum;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

/**
 * 移动平均线策略 (Moving Average Strategy) - 示例实现
 *
 * <p><b>类职责:</b></p>
 * <p>提供一个基于移动平均线交叉的信号生成策略。当前为模板实现，用于演示和测试策略调度、组合等核心流程。</p>
 *
 * <p><b>设计目的:</b></p>
 * <ol>
 *     <li>作为策略库中的一个基础模板，展示一个完整策略类应具备的结构。</li>
 *     <li>用于测试和验证策略引擎的调度、缓存、限流等AOP装饰功能是否正常工作。</li>
 *     <li>提供一个清晰的起点，便于后续开发人员接入真实行情数据，实现完整的均线策略逻辑。</li>
 * </ol>
 *
 * <p><b>为什么需要该类:</b></p>
 * <p>即使在未接入真实数据源的开发初期，一个结构完整、行为可预测的模板策略也是必要的。
 * 它可以保证上层调度和编排逻辑的开发和测试不受数据源缺失的影响，实现并行开发。</p>
 *
 * <p><b>核心实现思路 (待实现):</b></p>
 * <ol>
 *     <li><b>数据准备:</b> 获取股票池及各股票的历史收盘价。</li>
 *     <li><b>指标计算:</b> 计算短期（如5日）和长期（如20日）两条移动平均线。</li>
 *     <li><b>信号生成:</b>
 *         <ul>
 *             <li><b>金叉信号 (买入):</b> 当短期均线从下向上穿过长期均线时生成。</li>
 *             <li><b>死叉信号 (卖出):</b> 当短期均线从上向下穿过长期均线时生成。</li>
 *         </ul>
 *     </li>
 *     <li><b>结果构建:</b> 筛选出产生金叉信号的股票，并封装其相关信息（如交叉点、当前价格等）作为结果返回。</li>
 * </ol>
 *
 * @author hli
 * @date 2025-10-22
 */
@Slf4j
@Component
public class MovingAverageStrategy implements QuantStrategy {

    /**
     * 策略唯一标识
     */
    @Override
    public String getId() {
        return StrategyMetaEnum.SIG_MOVING_AVERAGE.getId();
    }

    /**
     * 执行均线策略（当前为模拟实现）
     *
     * <p><b>实现逻辑:</b></p>
     * <ol>
     *     <li>记录策略开始时间。</li>
     *     <li>通过 {@code Thread.sleep} 模拟真实计算或I/O操作的耗时，用于测试链路。</li>
     *     <li>生成一个随机值作为模拟的计算结果。</li>
     *     <li>将模拟结果封装成结构化的 Map。</li>
     *     <li>记录执行日志并构建包含模拟结果的 {@link StrategyResult}。</li>
     *     <li>在线程被中断时，恢复中断状态并记录错误日志。</li>
     * </ol>
     *
     * @param context 策略上下文，包含 userId、symbol、额外参数等
     * @return StrategyResult 策略执行结果，包含策略ID、数据和耗时
     */
    @Override
    public StrategyResult execute(StrategyContext context) {
        long start = System.currentTimeMillis();
        log.info("开始执行均线策略_模拟|Execute_moving_average_strategy_mock_start,context={}", context);

        try {
            // TODO: 真正实现需从DB或Feign调用历史行情数据计算均线
            // 模拟真实计算耗时，便于测试链路性能和超时
            Thread.sleep(1500);

            // 模拟计算结果
            double shortMA = 10 + Math.random() * 2; // 模拟短期均线
            double longMA = 11 - Math.random() * 2;  // 模拟长期均线

            Map<String, Object> mockData = new HashMap<>();
            mockData.put("signal_type", shortMA > longMA ? "Golden_Cross" : "Dead_Cross");
            mockData.put("short_ma", shortMA);
            mockData.put("long_ma", longMA);
            mockData.put("is_mock", true);

            log.info("均线策略模拟执行完成|Moving_average_strategy_mock_execution_finished,strategyId={},threadName={}",
                    getId(), Thread.currentThread().getName());

            return StrategyResult.builder()
                    .strategyId(getId())
                    .data(Collections.singletonList(mockData))
                    .durationMs(System.currentTimeMillis() - start)
                    .build();

        } catch (InterruptedException e) {
            // 恢复中断状态，这是处理InterruptedException的良好实践
            Thread.currentThread().interrupt();
            log.error("均线策略执行被中断|Moving_average_strategy_execution_interrupted", e);
            return buildErrorResult(start, "Strategy execution was interrupted.");
        } catch (Exception e) {
            log.error("均线策略执行失败|Moving_average_strategy_execution_failed", e);
            return buildErrorResult(start, e.getMessage());
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
