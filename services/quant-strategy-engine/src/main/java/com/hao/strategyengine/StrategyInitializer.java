package com.hao.strategyengine;

import com.hao.strategyengine.composite.CompositeStrategy;
import com.hao.strategyengine.core.Strategy;
import com.hao.strategyengine.core.StrategyContext;
import com.hao.strategyengine.engine.StrategyEngine;
import com.hao.strategyengine.factory.StrategyConfig;
import com.hao.strategyengine.factory.StrategyFactory;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.CommandLineRunner;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;

/**
 * 策略初始化器
 */
@Slf4j
@Component
public class StrategyInitializer implements CommandLineRunner {
    
    private final StrategyFactory strategyFactory;
    private final StrategyEngine strategyEngine;
    
    public StrategyInitializer(StrategyFactory strategyFactory, StrategyEngine strategyEngine) {
        this.strategyFactory = strategyFactory;
        this.strategyEngine = strategyEngine;
    }
    
    @Override
    public void run(String... args) throws Exception {
        log.info("开始初始化策略...");
        
        // 初始化示例策略
        initializeExampleStrategies();
        
        log.info("策略初始化完成");
    }
    
    /**
     * 初始化示例策略
     */
    private void initializeExampleStrategies() {
        // 1. 创建单一策略
        createSingleStrategy("AAPL", "MA");
        
        // 2. 创建组合策略
        createCompositeStrategy("TSLA");
        
        // 3. 创建自定义配置策略
        createCustomStrategy("NVDA", "MACD");
    }
    
    /**
     * 创建单一策略
     */
    private void createSingleStrategy(String symbol, String strategyType) {
        StrategyConfig config = StrategyConfig.defaultConfig(strategyType);
        config.setStrategyName(symbol + "_" + strategyType);
        
        Strategy strategy = strategyFactory.createStrategy(config);
        
        StrategyContext context = new StrategyContext();
        context.setSymbol(symbol);
        context.setAvailableFund(new BigDecimal("100000"));
        
        strategyEngine.registerStrategy(symbol, strategy, context);
        
        log.info("创建单一策略: symbol={}, type={}", symbol, strategyType);
    }
    
    /**
     * 创建组合策略
     */
    private void createCompositeStrategy(String symbol) {
        // 创建子策略
        StrategyConfig maConfig = StrategyConfig.defaultConfig("MA");
        Strategy maStrategy = strategyFactory.createStrategy(maConfig);
        
        StrategyConfig macdConfig = StrategyConfig.defaultConfig("MACD");
        Strategy macdStrategy = strategyFactory.createStrategy(macdConfig);
        
        // 创建组合策略（投票制）
        CompositeStrategy compositeStrategy = new CompositeStrategy(
            symbol + "_COMPOSITE", 
            CompositeStrategy.SignalMergeStrategy.VOTE
        );
        compositeStrategy.addStrategy(maStrategy);
        compositeStrategy.addStrategy(macdStrategy);
        
        StrategyContext context = new StrategyContext();
        context.setSymbol(symbol);
        context.setAvailableFund(new BigDecimal("200000"));
        
        strategyEngine.registerStrategy(symbol, compositeStrategy, context);
        
        log.info("创建组合策略: symbol={}, 子策略数=2", symbol);
    }
    
    /**
     * 创建自定义配置策略
     */
    private void createCustomStrategy(String symbol, String strategyType) {
        StrategyConfig config = new StrategyConfig();
        config.setStrategyType(strategyType);
        config.setStrategyName(symbol + "_CUSTOM_" + strategyType);
        
        // 自定义止损止盈参数
        config.setEnableStopLoss(true);
        config.setStopLossRatio(new BigDecimal("0.03"));  // 3%止损
        config.setEnableTakeProfit(true);
        config.setTakeProfitRatio(new BigDecimal("0.15")); // 15%止盈
        
        // 自定义仓位管理
        config.setEnablePositionSize(true);
        config.setMaxPositionRatio(new BigDecimal("0.25")); // 25%最大仓位
        
        Strategy strategy = strategyFactory.createStrategy(config);
        
        StrategyContext context = new StrategyContext();
        context.setSymbol(symbol);
        context.setAvailableFund(new BigDecimal("150000"));
        
        strategyEngine.registerStrategy(symbol, strategy, context);
        
        log.info("创建自定义策略: symbol={}, type={}", symbol, strategyType);
    }
}
