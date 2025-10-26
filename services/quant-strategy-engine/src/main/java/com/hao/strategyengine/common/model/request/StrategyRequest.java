package com.hao.strategyengine.common.model.request;

import lombok.Data;
import java.util.List;
import java.util.Map;

/**
 * StrategyRequest 表示客户端请求策略引擎时所携带的请求参数对象。
 *
 * <p>该对象用于封装用户、交易标的及策略信息，支持扩展字段以满足不同策略场景的需求。</p>
 *
 * <p>示例用法：</p>
 * <pre>{@code
 * StrategyRequest request = new StrategyRequest();
 * request.setUserId("user123");
 * request.setSymbol("AAPL");
 * request.setStrategyIds(Arrays.asList("strategy1", "strategy2"));
 * request.setExtra(Map.of("leverage", 2, "riskLevel", "medium"));
 * }</pre>
 */
@Data
public class StrategyRequest {

    /**
     * 用户唯一标识
     * 用于区分不同用户的请求。
     */
    private Integer userId;

    /**
     * 交易标的（股票、期货等）代码
     * 例如股票代码 "AAPL"、"000001.SZ"。
     */
    private String symbol;

    /**
     * 策略ID列表
     * 该请求要执行的策略集合，可指定多个策略。
     */
    private List<String> strategyIds;

    /**
     * 扩展字段
     * 可用于存放额外的请求参数，例如杠杆倍数、风险等级、交易时间窗口等。
     * Key 为参数名，Value 为参数值。
     */
    private Map<String, Object> extra;
}
