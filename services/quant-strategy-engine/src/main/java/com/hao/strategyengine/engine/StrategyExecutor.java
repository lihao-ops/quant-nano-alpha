package com.hao.strategyengine.engine;

import com.hao.strategyengine.model.Signal;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

/**
 * 策略执行器
 * 负责将信号转换为实际订单
 */
@Slf4j
@Component
public class StrategyExecutor {
    
    // TODO: 注入订单服务客户端
    // @Autowired
    // private OrderServiceClient orderServiceClient;
    
    /**
     * 执行交易信号
     */
    public void executeSignal(Signal signal) {
        log.info("执行交易信号: symbol={}, type={}, price={}, quantity={}", 
            signal.getSymbol(), signal.getType(), signal.getPrice(), signal.getQuantity());
        
        try {
            switch (signal.getType()) {
                case BUY:
                    executeBuy(signal);
                    break;
                case SELL:
                    executeSell(signal);
                    break;
                case CLOSE:
                    executeClose(signal);
                    break;
                default:
                    log.warn("未知信号类型: {}", signal.getType());
            }
        } catch (Exception e) {
            log.error("执行信号失败: {}", signal, e);
        }
    }
    
    /**
     * 执行买入
     */
    private void executeBuy(Signal signal) {
        log.info("提交买入订单: symbol={}, price={}, quantity={}", 
            signal.getSymbol(), signal.getPrice(), signal.getQuantity());
        
        // TODO: 调用订单服务
        // OrderRequest request = OrderRequest.builder()
        //     .symbol(signal.getSymbol())
        //     .side(OrderSide.BUY)
        //     .price(signal.getPrice())
        //     .quantity(signal.getQuantity())
        //     .build();
        // orderServiceClient.createOrder(request);
    }
    
    /**
     * 执行卖出
     */
    private void executeSell(Signal signal) {
        log.info("提交卖出订单: symbol={}, price={}, quantity={}", 
            signal.getSymbol(), signal.getPrice(), signal.getQuantity());
        
        // TODO: 调用订单服务
    }
    
    /**
     * 执行平仓
     */
    private void executeClose(Signal signal) {
        log.info("提交平仓订单: symbol={}", signal.getSymbol());
        
        // TODO: 调用订单服务
    }
}
