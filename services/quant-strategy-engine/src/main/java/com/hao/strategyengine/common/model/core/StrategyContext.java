package com.hao.strategyengine.common.model.core;

import lombok.Builder;
import lombok.Data;
import java.time.Instant;
import java.util.Map;

/**
 * StrategyContext
 *
 * <p>策略执行上下文对象，用于封装策略执行所需的所有信息。</p>
 *
 * <p>字段说明：</p>
 * <ul>
 *     <li>userId - 用户唯一标识，用于区分不同用户的请求</li>
 *     <li>symbol - 交易标的代码，如股票、期货等</li>
 *     <li>extra - 扩展字段，可存储策略执行所需的额外参数，如杠杆倍数、风控等级等</li>
 *     <li>requestTime - 请求时间，用于策略执行时间窗口或日志追踪</li>
 * </ul>
 *
 * <p>示例用法：</p>
 * <pre>{@code
 * StrategyContext ctx = StrategyContext.builder()
 *     .userId("user123")
 *     .symbol("AAPL")
 *     .extra(Map.of("leverage", 2, "riskLevel", "medium"))
 *     .requestTime(Instant.now())
 *     .build();
 * }</pre>
 *
 * <p>注意事项：</p>
 * <ul>
 *     <li>extra 字段建议使用约定好的 key，以保证策略执行一致性</li>
 *     <li>requestTime 可用于执行限时策略或计算请求延迟</li>
 * </ul>
 *
 * Lombok 注解说明：
 * - @Data 提供 getter、setter、toString、equals、hashCode 方法
 * - @Builder 提供建造者模式方便构造对象
 * </p>
 *
 * @author hli
 * @date 2025-10-22
 */
@Data
@Builder
public class StrategyContext {

    /** 用户唯一标识 */
    private String userId;

    /** 交易标的代码，例如股票代码 "AAPL" */
    private String symbol;

    /** 扩展字段，用于存储策略执行所需的额外参数 */
    private Map<String, Object> extra;

    /** 请求时间，记录策略执行发起的时间 */
    private Instant requestTime;
}
