package com.hao.strategyengine.event.listener;


import com.hao.strategyengine.event.StrategyEvent;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

/**
 * 日志记录监听器
 */
@Slf4j
@Component
public class LoggingEventListener implements StrategyEventListener {
    
    @Override
    public void onEvent(StrategyEvent event) {
        switch (event.getType()) {
            case SIGNAL_GENERATED:
                log.info("策略信号: strategy={}, symbol={}, data={}", 
                    event.getStrategyName(), event.getSymbol(), event.getData());
                break;
            case ORDER_EXECUTED:
                log.info("订单执行: strategy={}, symbol={}, data={}", 
                    event.getStrategyName(), event.getSymbol(), event.getData());
                break;
            case RISK_REJECTED:
                log.warn("风控拒绝: strategy={}, symbol={}, data={}", 
                    event.getStrategyName(), event.getSymbol(), event.getData());
                break;
            case STRATEGY_ERROR:
                log.error("策略错误: strategy={}, symbol={}, data={}", 
                    event.getStrategyName(), event.getSymbol(), event.getData());
                break;
        }
    }
    
    @Override
    public String getName() {
        return "LoggingEventListener";
    }
}
