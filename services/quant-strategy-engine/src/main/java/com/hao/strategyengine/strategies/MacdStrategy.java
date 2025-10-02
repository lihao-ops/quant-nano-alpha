package com.hao.strategyengine.strategies;


import com.hao.strategyengine.model.Signal;
import com.hao.strategyengine.core.StrategyContext;
import com.hao.strategyengine.template.AbstractStrategy;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.util.LinkedList;
import java.util.Queue;

/**
 * MACD策略
 */
@Slf4j
@Component
public class MacdStrategy extends AbstractStrategy {
    
    private Queue<BigDecimal> priceQueue;
    private BigDecimal ema12;
    private BigDecimal ema26;
    private BigDecimal previousDiff;
    
    public MacdStrategy() {
        super("MACD_STRATEGY");
        this.priceQueue = new LinkedList<>();
    }
    
    @Override
    public void initialize(StrategyContext context) {
        context.setParameter("ema12Period", 12);
        context.setParameter("ema26Period", 26);
        context.setParameter("deaPeriod", 9);
        this.ready = true;
        log.info("MACD策略初始化完成");
    }
    
    @Override
    protected void calculateIndicators(StrategyContext context) {
        BigDecimal currentPrice = context.getCurrentData().getPrice();
        priceQueue.offer(currentPrice);
        
        if (priceQueue.size() > 26) {
            priceQueue.poll();
        }
        
        // 计算EMA12和EMA26
        if (ema12 == null) {
            ema12 = currentPrice;
            ema26 = currentPrice;
        } else {
            ema12 = calculateEma(currentPrice, ema12, 12);
            ema26 = calculateEma(currentPrice, ema26, 26);
        }
        
        // 计算DIFF (DIF = EMA12 - EMA26)
        BigDecimal diff = ema12.subtract(ema26);
        
        context.getState().put("diff", diff);
        context.getState().put("previousDiff", previousDiff);
        
        previousDiff = diff;
    }
    
    @Override
    protected Signal generateSignal(StrategyContext context) {
        if (priceQueue.size() < 26 || previousDiff == null) {
            return Signal.hold(context.getSymbol());
        }
        
        BigDecimal diff = (BigDecimal) context.getState().get("diff");
        BigDecimal prevDiff = (BigDecimal) context.getState().get("previousDiff");
        
        Signal signal = new Signal();
        signal.setSymbol(context.getSymbol());
        signal.setPrice(context.getCurrentData().getPrice());
        
        // DIFF上穿0轴：买入信号
        if (prevDiff.compareTo(BigDecimal.ZERO) < 0 && 
            diff.compareTo(BigDecimal.ZERO) > 0) {
            signal.setType(Signal.SignalType.BUY);
            signal.setConfidence(80);
            signal.setReason("MACD DIFF上穿0轴");
        }
        // DIFF下穿0轴：卖出信号
        else if (prevDiff.compareTo(BigDecimal.ZERO) > 0 && 
                 diff.compareTo(BigDecimal.ZERO) < 0) {
            signal.setType(Signal.SignalType.SELL);
            signal.setConfidence(80);
            signal.setReason("MACD DIFF下穿0轴");
        }
        else {
            signal.setType(Signal.SignalType.HOLD);
            signal.setConfidence(50);
        }
        
        return signal;
    }
    
    private BigDecimal calculateEma(BigDecimal price, BigDecimal prevEma, int period) {
        BigDecimal multiplier = BigDecimal.valueOf(2.0 / (period + 1));
        return price.subtract(prevEma)
            .multiply(multiplier)
            .add(prevEma);
    }
}
