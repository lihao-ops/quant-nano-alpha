package com.hao.strategyengine.decorator;

import com.hao.strategyengine.model.Signal;
import com.hao.strategyengine.core.Strategy;
import com.hao.strategyengine.core.StrategyContext;
import lombok.extern.slf4j.Slf4j;

import java.math.BigDecimal;

/**
 * 止损装饰器
 */
@Slf4j
public class StopLossDecorator extends StrategyDecorator {
    
    private BigDecimal stopLossRatio;  // 止损比例
    private BigDecimal entryPrice;     // 入场价格
    
    public StopLossDecorator(Strategy strategy, BigDecimal stopLossRatio) {
        super(strategy);
        this.stopLossRatio = stopLossRatio;
    }
    
    @Override
    protected String getDecoratorName() {
        return "STOP_LOSS";
    }
    
    @Override
    protected Signal decorate(StrategyContext context, Signal signal) {
        // 记录入场价格
        if (signal.getType() == Signal.SignalType.BUY) {
            entryPrice = signal.getPrice();
            log.info("记录入场价格: {}", entryPrice);
        }
        
        // 检查止损条件
        if (entryPrice != null && context.getCurrentPosition().compareTo(BigDecimal.ZERO) > 0) {
            BigDecimal currentPrice = context.getCurrentData().getPrice();
            BigDecimal lossRatio = entryPrice.subtract(currentPrice)
                .divide(entryPrice, 4, BigDecimal.ROUND_HALF_UP);
            
            // 触发止损
            if (lossRatio.compareTo(stopLossRatio) >= 0) {
                log.warn("触发止损！入场价: {}, 当前价: {}, 亏损比例: {}%", 
                    entryPrice, currentPrice, lossRatio.multiply(BigDecimal.valueOf(100)));
                
                Signal stopLossSignal = new Signal();
                stopLossSignal.setSymbol(signal.getSymbol());
                stopLossSignal.setType(Signal.SignalType.SELL);
                stopLossSignal.setPrice(currentPrice);
                stopLossSignal.setQuantity(context.getCurrentPosition());
                stopLossSignal.setConfidence(100);
                stopLossSignal.setReason("触发止损，亏损比例: " + lossRatio.multiply(BigDecimal.valueOf(100)) + "%");
                
                entryPrice = null; // 重置入场价
                return stopLossSignal;
            }
        }
        
        return signal;
    }
}
