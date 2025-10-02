package com.hao.strategyengine.engine;

import com.hao.strategyengine.chain.RiskFilter;
import com.hao.strategyengine.model.market.MarketData;
import com.hao.strategyengine.model.Signal;
import com.hao.strategyengine.core.Strategy;
import com.hao.strategyengine.core.StrategyContext;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * 策略引擎
 * 管理策略的生命周期和执行
 */
@Slf4j
@Component
public class StrategyEngine {
    
    private Map<String, Strategy> strategies = new ConcurrentHashMap<>();
    private Map<String, StrategyContext> contexts = new ConcurrentHashMap<>();
    private RiskFilter riskFilterChain;
    private StrategyExecutor executor;
    
    public StrategyEngine(StrategyExecutor executor) {
        this.executor = executor;
    }
    
    /**
     * 设置风控过滤链
     */
    public void setRiskFilterChain(RiskFilter riskFilterChain) {
        this.riskFilterChain = riskFilterChain;
        log.info("设置风控过滤链");
    }
    
    /**
     * 注册策略
     */
    public void registerStrategy(String symbol, Strategy strategy, StrategyContext context) {
        strategies.put(symbol, strategy);
        contexts.put(symbol, context);
        
        // 初始化策略
        strategy.initialize(context);
        
        log.info("注册策略: symbol={}, strategy={}", symbol, strategy.getName());
    }
    
    /**
     * 注销策略
     */
    public void unregisterStrategy(String symbol) {
        strategies.remove(symbol);
        contexts.remove(symbol);
        log.info("注销策略: symbol={}", symbol);
    }
    
    /**
     * 处理市场数据
     */
    public void onMarketData(MarketData marketData) {
        String symbol = marketData.getSymbol();
        
        Strategy strategy = strategies.get(symbol);
        StrategyContext context = contexts.get(symbol);
        
        if (strategy == null || context == null) {
            log.debug("未找到策略配置: {}", symbol);
            return;
        }
        
        if (!strategy.isReady()) {
            log.warn("策略未就绪: {}", strategy.getName());
            return;
        }
        
        try {
            // 更新上下文
            context.setCurrentData(marketData);
            
            // 执行策略分析
            Signal signal = strategy.analyze(context);
            
            log.info("策略信号: symbol={}, type={}, confidence={}, reason={}", 
                symbol, signal.getType(), signal.getConfidence(), signal.getReason());
            
            // 处理信号
            processSignal(context, signal);
            
        } catch (Exception e) {
            log.error("策略执行失败: symbol={}, strategy={}", symbol, strategy.getName(), e);
        }
    }
    
    /**
     * 处理交易信号
     */
    private void processSignal(StrategyContext context, Signal signal) {
        // 忽略HOLD信号
        if (signal.getType() == Signal.SignalType.HOLD) {
            return;
        }
        
        // 风控检查
        if (riskFilterChain != null) {
            RiskFilter.FilterResult filterResult = riskFilterChain.filter(context, signal);
            
            if (!filterResult.isPassed()) {
                log.warn("风控拒绝: filter={}, reason={}", 
                    filterResult.getFilterName(), filterResult.getReason());
                return;
            }
        }
        
        // 执行订单
        executor.executeSignal(signal);
    }
    
    /**
     * 获取所有策略状态
     */
    public Map<String, String> getStrategyStatus() {
        Map<String, String> status = new ConcurrentHashMap<>();
        
        strategies.forEach((symbol, strategy) -> {
            status.put(symbol, strategy.isReady() ? "READY" : "NOT_READY");
        });
        
        return status;
    }
    
    /**
     * 暂停所有策略
     */
    public void pauseAll() {
        log.info("暂停所有策略");
        // TODO: 实现暂停逻辑
    }
    
    /**
     * 恢复所有策略
     */
    public void resumeAll() {
        log.info("恢复所有策略");
        // TODO: 实现恢复逻辑
    }
}
