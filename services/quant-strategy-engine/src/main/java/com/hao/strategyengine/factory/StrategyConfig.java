package com.hao.strategyengine.factory;

import lombok.Data;
import java.math.BigDecimal;
import java.util.HashMap;
import java.util.Map;

/**
 * 策略配置
 */
@Data
public class StrategyConfig {
    private String strategyType;              // 策略类型
    private String strategyName;              // 策略名称
    private Map<String, Object> parameters;   // 策略参数
    private boolean enableStopLoss;           // 启用止损
    private BigDecimal stopLossRatio;         // 止损比例
    private boolean enableTakeProfit;         // 启用止盈
    private BigDecimal takeProfitRatio;       // 止盈比例
    private boolean enablePositionSize;       // 启用仓位管理
    private BigDecimal maxPositionRatio;      // 最大仓位比例
    
    public StrategyConfig() {
        this.parameters = new HashMap<>();
    }
    
    public static StrategyConfig defaultConfig(String strategyType) {
        StrategyConfig config = new StrategyConfig();
        config.setStrategyType(strategyType);
        config.setStrategyName(strategyType + "_DEFAULT");
        config.setEnableStopLoss(true);
        config.setStopLossRatio(new BigDecimal("0.05"));  // 5%止损
        config.setEnableTakeProfit(true);
        config.setTakeProfitRatio(new BigDecimal("0.10")); // 10%止盈
        config.setEnablePositionSize(true);
        config.setMaxPositionRatio(new BigDecimal("0.30")); // 30%最大仓位
        return config;
    }
}
