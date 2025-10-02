package com.hao.strategyengine.decorator;

import com.hao.strategyengine.model.Signal;
import com.hao.strategyengine.core.Strategy;
import com.hao.strategyengine.core.StrategyContext;
import lombok.extern.slf4j.Slf4j;

import java.math.BigDecimal;

/**
 * 止盈装饰器
 */
@Slf4j
public class TakeProfitDecorator extends StrategyDecorator {
    
    private BigDecimal takeProfitRatio;  // 止盈比例
    private BigDecimal entryPrice;       // 入场价格
    
    public TakeProfitDecorator(Strategy strategy, BigDecimal takeProfitRatio) {
        super(strategy);
        this.takeProfitRatio = takeProfitRatio;
    }
    
    @Override
    protected String getDecoratorName() {
        return "TAKE_PROFIT";
    }
    
    @Override
    protected Signal decorate(StrategyContext context, Signal signal) {
        if (signal.getType() == Signal.SignalType.BUY) {
            entryPrice = signal.getPrice();
            log.info("记录止盈入场价格: {}", entryPrice);
        }
        
        if (entryPrice != null && context.getCurrentPosition().compareTo(BigDecimal.ZERO) > 0) {
            BigDecimal currentPrice = context.getCurrentData().getPrice();
            BigDecimal profitRatio = currentPrice.subtract(entryPrice)
                .divide(entryPrice, 4, BigDecimal.ROUND_HALF_UP);
            
            // 触发止盈
            if (profitRatio.compareTo(takeProfitRatio) >= 0) {
                log.info("触发止盈！入场价: {}, 当前价: {}, 盈利比例: {}%", 
                    entryPrice, currentPrice, profitRatio.multiply(BigDecimal.valueOf(100)));
                
                Signal takeProfitSignal = new Signal();
                takeProfitSignal.setSymbol(signal.getSymbol());
                takeProfitSignal.setType(Signal.SignalType.SELL);
                takeProfitSignal.setPrice(currentPrice);
                takeProfitSignal.setQuantity(context.getCurrentPosition());
                takeProfitSignal.setConfidence(100);
                takeProfitSignal.setReason("触发止盈，盈利比例: " + profitRatio.multiply(BigDecimal.valueOf(100)) + "%");
                
                entryPrice = null;
                return takeProfitSignal;
            }
        }
        
        return signal;
    }
}
