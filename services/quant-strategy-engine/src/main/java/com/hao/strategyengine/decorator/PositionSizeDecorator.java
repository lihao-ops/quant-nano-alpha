package com.hao.strategyengine.decorator;

import com.hao.strategyengine.model.Signal;
import com.hao.strategyengine.core.Strategy;
import com.hao.strategyengine.core.StrategyContext;
import lombok.extern.slf4j.Slf4j;

import java.math.BigDecimal;
import java.math.RoundingMode;

/**
 * 仓位管理装饰器
 * 根据凯利公式或固定比例管理仓位
 */
@Slf4j
public class PositionSizeDecorator extends StrategyDecorator {
    
    private BigDecimal maxPositionRatio;  // 最大仓位比例
    
    public PositionSizeDecorator(Strategy strategy, BigDecimal maxPositionRatio) {
        super(strategy);
        this.maxPositionRatio = maxPositionRatio;
    }
    
    @Override
    protected String getDecoratorName() {
        return "POSITION_SIZE";
    }
    
    @Override
    protected Signal decorate(StrategyContext context, Signal signal) {
        if (signal.getType() == Signal.SignalType.BUY) {
            // 计算建议仓位
            BigDecimal availableFund = context.getAvailableFund();
            BigDecimal maxInvestment = availableFund.multiply(maxPositionRatio);
            
            // 根据信号强度调整仓位
            BigDecimal confidenceMultiplier = BigDecimal.valueOf(signal.getConfidence())
                .divide(BigDecimal.valueOf(100), 2, RoundingMode.HALF_UP);
            
            BigDecimal investment = maxInvestment.multiply(confidenceMultiplier);
            
            // 计算购买数量
            BigDecimal quantity = investment.divide(signal.getPrice(), 0, RoundingMode.DOWN);
            
            signal.setQuantity(quantity);
            
            log.info("仓位管理：可用资金={}, 最大投资={}, 信号强度={}%, 实际投资={}, 购买数量={}", 
                availableFund, maxInvestment, signal.getConfidence(), investment, quantity);
        }
        
        return signal;
    }
}
