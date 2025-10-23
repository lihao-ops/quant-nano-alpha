package com.hao.strategyengine.common.model.response;

import lombok.Builder;
import lombok.Data;

/**
 * StrategyResult
 *
 * <p>策略执行结果对象，用于封装单个策略的输出数据和执行信息。</p>
 *
 * <p>字段说明：</p>
 * <ul>
 *     <li>strategyId - 策略ID，用于标识执行的是哪一个策略</li>
 *     <li>data - 策略执行返回的数据，可为任意类型，具体类型取决于策略实现</li>
 *     <li>durationMs - 策略执行耗时（毫秒），用于性能监控和分析</li>
 * </ul>
 *
 * <p>示例用法：</p>
 * <pre>{@code
 * StrategyResult result = StrategyResult.builder()
 *     .strategyId("strategy1")
 *     .data(Map.of("signal", "BUY", "score", 0.85))
 *     .durationMs(120)
 *     .build();
 * }</pre>
 *
 * <p>注意事项：</p>
 * <ul>
 *     <li>data 类型应与策略约定一致，必要时可封装成统一的 DTO 类，避免 Object 的类型转换问题</li>
 *     <li>durationMs 可用于监控慢策略，做限时优化或报警</li>
 * </ul>
 *
 * Lombok 注解说明：
 * - @Data 提供 getter、setter、toString、equals、hashCode 方法
 * - @Builder 提供建造者模式，方便构造实例
 * </p>
 *
 * @author hli
 * @date 2025-10-22
 */
@Data
@Builder
public class StrategyResult {

    /** 策略ID */
    private String strategyId;

    /** 策略返回数据 */
    private Object data;

    /** 策略执行耗时（毫秒） */
    private long durationMs;
}
