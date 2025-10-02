package com.hao.strategyengine.model;

import lombok.Data;
import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * 交易信号
 */
@Data
public class Signal {
    public enum SignalType {
        BUY,      // 买入
        SELL,     // 卖出
        HOLD,     // 持有
        CLOSE     // 平仓
    }
    
    private String symbol;                // 交易标的
    private SignalType type;              // 信号类型
    private BigDecimal price;             // 建议价格
    private BigDecimal quantity;          // 建议数量
    private Integer confidence;           // 信号强度(0-100)
    private String strategyName;          // 策略名称
    private LocalDateTime timestamp;      // 时间戳
    private String reason;                // 信号原因
    
    public static Signal hold(String symbol) {
        Signal signal = new Signal();
        signal.setSymbol(symbol);
        signal.setType(SignalType.HOLD);
        signal.setTimestamp(LocalDateTime.now());
        return signal;
    }
}
