package com.hao.strategyengine.strategies;


import com.hao.strategyengine.model.Signal;
import com.hao.strategyengine.core.StrategyContext;
import com.hao.strategyengine.template.AbstractStrategy;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.util.*;

/**
 * 双均线策略
 */
@Slf4j
@Component
public class MaStrategy extends AbstractStrategy {
    
    private static final String SHORT_PERIOD = "shortPeriod";
    private static final String LONG_PERIOD = "longPeriod";
    
    private Queue<BigDecimal> shortQueue;
    private Queue<BigDecimal> longQueue;
    
    public MaStrategy() {
        super("MA_STRATEGY");
        this.shortQueue = new LinkedList<>();
        this.longQueue = new LinkedList<>();
    }
    
    @Override
    public void initialize(StrategyContext context) {
        // 设置默认参数
        context.setParameter(SHORT_PERIOD, 5);
        context.setParameter(LONG_PERIOD, 20);
        this.ready = true;
        log.info("均线策略初始化完成");
    }
    
    @Override
    protected void calculateIndicators(StrategyContext context) {
        BigDecimal currentPrice = context.getCurrentData().getPrice();
        Integer shortPeriod = (Integer) context.getParameter(SHORT_PERIOD);
        Integer longPeriod = (Integer) context.getParameter(LONG_PERIOD);
        
        // 维护短期均线队列
        shortQueue.offer(currentPrice);
        if (shortQueue.size() > shortPeriod) {
            shortQueue.poll();
        }
        
        // 维护长期均线队列
        longQueue.offer(currentPrice);
        if (longQueue.size() > longPeriod) {
            longQueue.poll();
        }
        
        // 计算均线值
        BigDecimal shortMa = calculateAverage(shortQueue);
        BigDecimal longMa = calculateAverage(longQueue);
        
        // 保存到上下文
        context.getState().put("shortMa", shortMa);
        context.getState().put("longMa", longMa);
        context.getState().put("price", currentPrice);
    }
    
    @Override
    protected Signal generateSignal(StrategyContext context) {
        // 数据不足
        if (shortQueue.size() < (Integer) context.getParameter(SHORT_PERIOD) ||
            longQueue.size() < (Integer) context.getParameter(LONG_PERIOD)) {
            return Signal.hold(context.getSymbol());
        }
        
        BigDecimal shortMa = (BigDecimal) context.getState().get("shortMa");
        BigDecimal longMa = (BigDecimal) context.getState().get("longMa");
        BigDecimal price = (BigDecimal) context.getState().get("price");
        
        Signal signal = new Signal();
        signal.setSymbol(context.getSymbol());
        signal.setPrice(price);
        
        // 金叉：短期均线上穿长期均线
        if (shortMa.compareTo(longMa) > 0) {
            signal.setType(Signal.SignalType.BUY);
            signal.setConfidence(70);
            signal.setReason(String.format("金叉：短期均线(%.2f) > 长期均线(%.2f)", 
                shortMa, longMa));
        } 
        // 死叉：短期均线下穿长期均线
        else if (shortMa.compareTo(longMa) < 0) {
            signal.setType(Signal.SignalType.SELL);
            signal.setConfidence(70);
            signal.setReason(String.format("死叉：短期均线(%.2f) < 长期均线(%.2f)", 
                shortMa, longMa));
        } 
        else {
            signal.setType(Signal.SignalType.HOLD);
            signal.setConfidence(50);
        }

        return signal;
    }

    private BigDecimal calculateAverage(Queue<BigDecimal> queue) {
        if (queue.isEmpty()) {
            return BigDecimal.ZERO;
        }
        BigDecimal sum = queue.stream()
                .reduce(BigDecimal.ZERO, BigDecimal::add);
        return sum.divide(BigDecimal.valueOf(queue.size()), 2, BigDecimal.ROUND_HALF_UP);
    }
}