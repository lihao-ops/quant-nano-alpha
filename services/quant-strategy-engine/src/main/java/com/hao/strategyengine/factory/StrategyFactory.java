package com.hao.strategyengine.factory;

import com.hao.strategyengine.core.Strategy;
import com.hao.strategyengine.decorator.PositionSizeDecorator;
import com.hao.strategyengine.decorator.StopLossDecorator;
import com.hao.strategyengine.decorator.TakeProfitDecorator;
import com.hao.strategyengine.strategies.LongOneStrategy;
import com.hao.strategyengine.strategies.MacdStrategy;
import com.hao.strategyengine.strategies.MaStrategy;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.HashMap;
import java.util.Map;

/**
 * 策略工厂
 * 使用工厂模式创建策略实例
 */
@Slf4j
@Component
public class StrategyFactory {
    
    private Map<String, Class<? extends Strategy>> strategyRegistry = new HashMap<>();
    
    public StrategyFactory() {
        // 注册内置策略
        registerStrategy("MA", MaStrategy.class);
        registerStrategy("MACD", MacdStrategy.class);
        registerStrategy("LongOne", LongOneStrategy.class);
        // 可继续注册其他策略...
    }
    
    /**
     * 注册策略类型
     */
    public void registerStrategy(String type, Class<? extends Strategy> strategyClass) {
        strategyRegistry.put(type, strategyClass);
        log.info("注册策略类型: {} -> {}", type, strategyClass.getSimpleName());
    }
    
    /**
     * 创建策略实例
     */
    public Strategy createStrategy(StrategyConfig config) {
        try {
            // 1. 创建基础策略
            Strategy baseStrategy = createBaseStrategy(config);
            
            // 2. 应用装饰器
            Strategy decoratedStrategy = applyDecorators(baseStrategy, config);
            
            log.info("创建策略成功: {}", decoratedStrategy.getName());
            return decoratedStrategy;
            
        } catch (Exception e) {
            log.error("创建策略失败: {}", config.getStrategyType(), e);
            throw new RuntimeException("策略创建失败", e);
        }
    }
    
    /**
     * 创建基础策略
     */
    private Strategy createBaseStrategy(StrategyConfig config) throws Exception {
        String type = config.getStrategyType();
        Class<? extends Strategy> strategyClass = strategyRegistry.get(type);
        
        if (strategyClass == null) {
            throw new IllegalArgumentException("未知的策略类型: " + type);
        }
        
        return strategyClass.getDeclaredConstructor().newInstance();
    }
    
    /**
     * 应用装饰器
     */
    private Strategy applyDecorators(Strategy strategy, StrategyConfig config) {
        Strategy result = strategy;
        
        // 应用止损装饰器
        if (config.isEnableStopLoss()) {
            result = new StopLossDecorator(result, config.getStopLossRatio());
            log.debug("应用止损装饰器，比例: {}", config.getStopLossRatio());
        }
        
        // 应用止盈装饰器
        if (config.isEnableTakeProfit()) {
            result = new TakeProfitDecorator(result, config.getTakeProfitRatio());
            log.debug("应用止盈装饰器，比例: {}", config.getTakeProfitRatio());
        }
        
        // 应用仓位管理装饰器
        if (config.isEnablePositionSize()) {
            result = new PositionSizeDecorator(result, config.getMaxPositionRatio());
            log.debug("应用仓位管理装饰器，最大仓位: {}", config.getMaxPositionRatio());
        }
        
        return result;
    }
    
    /**
     * 批量创建策略
     */
    public Map<String, Strategy> createStrategies(Map<String, StrategyConfig> configs) {
        Map<String, Strategy> strategies = new HashMap<>();
        
        configs.forEach((name, config) -> {
            Strategy strategy = createStrategy(config);
            strategies.put(name, strategy);
        });
        
        log.info("批量创建策略完成，共{}个", strategies.size());
        return strategies;
    }
}
