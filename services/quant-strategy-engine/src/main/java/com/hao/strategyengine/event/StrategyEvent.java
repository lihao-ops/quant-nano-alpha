package com.hao.strategyengine.event;

import lombok.Data;
import java.time.LocalDateTime;

/**
 * 策略事件
 */
@Data
public class StrategyEvent {
    
    public enum EventType {
        SIGNAL_GENERATED,    // 信号生成
        ORDER_EXECUTED,      // 订单执行
        RISK_REJECTED,       // 风控拒绝
        STRATEGY_ERROR       // 策略错误
    }
    
    private EventType type;
    private String strategyName;
    private String symbol;
    private Object data;
    private LocalDateTime timestamp;
    
    public static StrategyEvent signalGenerated(String strategyName, String symbol, Object signal) {
        StrategyEvent event = new StrategyEvent();
        event.setType(EventType.SIGNAL_GENERATED);
        event.setStrategyName(strategyName);
        event.setSymbol(symbol);
        event.setData(signal);
        event.setTimestamp(LocalDateTime.now());
        return event;
    }
}
