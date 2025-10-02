package com.hao.strategyengine;

import com.hao.strategyengine.composite.CompositeStrategy;
import com.hao.strategyengine.core.Strategy;
import com.hao.strategyengine.core.StrategyContext;
import com.hao.strategyengine.factory.StrategyConfig;
import com.hao.strategyengine.factory.StrategyFactory;
import com.hao.strategyengine.model.Signal;
import com.hao.strategyengine.model.market.MarketData;
import lombok.extern.slf4j.Slf4j;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * 策略使用示例
 */
@Slf4j
public class StrategyUsageExample {
    
    public static void main(String[] args) {
        exampleBasicStrategy();
        exampleCompositeStrategy();
        exampleCustomStrategy();
    }
    
    /**
     * 示例1：基础策略使用
     */
    public static void exampleBasicStrategy() {
        log.info("=== 示例1：基础策略使用 ===");
        
        // 1. 创建策略工厂
        StrategyFactory factory = new StrategyFactory();
        
        // 2. 使用默认配置创建MA策略
        StrategyConfig config = StrategyConfig.defaultConfig("MA");
        Strategy strategy = factory.createStrategy(config);
        
        // 3. 初始化策略上下文
        StrategyContext context = new StrategyContext();
        context.setSymbol("AAPL");
        context.setAvailableFund(new BigDecimal("100000"));
        
        // 4. 初始化策略
        strategy.initialize(context);
        
        // 5. 模拟市场数据
        MarketData marketData = new MarketData();
        marketData.setSymbol("AAPL");
        marketData.setPrice(new BigDecimal("150.50"));
        marketData.setVolume(new BigDecimal(1000000L));
        marketData.setTimestamp(LocalDateTime.now());
        
        context.setCurrentData(marketData);
        
        // 6. 分析生成信号
        if (strategy.isReady()) {
            Signal signal = strategy.analyze(context);
            log.info("生成信号: type={}, confidence={}, reason={}", 
                signal.getType(), signal.getConfidence(), signal.getReason());
        }
    }
    
    /**
     * 示例2：组合策略使用
     */
    public static void exampleCompositeStrategy() {
        log.info("=== 示例2：组合策略使用 ===");
        
        StrategyFactory factory = new StrategyFactory();
        
        // 创建多个子策略
        Strategy maStrategy = factory.createStrategy(
            StrategyConfig.defaultConfig("MA"));
        Strategy macdStrategy = factory.createStrategy(
            StrategyConfig.defaultConfig("MACD"));
        
        // 创建组合策略（投票制）
        CompositeStrategy compositeStrategy = new CompositeStrategy(
            "COMPOSITE_STRATEGY", 
            CompositeStrategy.SignalMergeStrategy.VOTE
        );
        compositeStrategy.addStrategy(maStrategy);
        compositeStrategy.addStrategy(macdStrategy);
        
        // 初始化并使用
        StrategyContext context = new StrategyContext();
        context.setSymbol("TSLA");
        context.setAvailableFund(new BigDecimal("200000"));
        
        compositeStrategy.initialize(context);
        
//        log.info("组合策略创建成功，包含{}个子策略", compositeStrategy.getSubStrategies().size());
    }
    
    /**
     * 示例3：自定义策略配置
     */
    public static void exampleCustomStrategy() {
        log.info("=== 示例3：自定义策略配置 ===");
        
        StrategyFactory factory = new StrategyFactory();
        
        // 创建自定义配置
        StrategyConfig config = new StrategyConfig();
        config.setStrategyType("MACD");
        config.setStrategyName("CUSTOM_MACD");
        
        // 自定义风控参数
        config.setEnableStopLoss(true);
        config.setStopLossRatio(new BigDecimal("0.03"));  // 3%止损
        
        config.setEnableTakeProfit(true);
        config.setTakeProfitRatio(new BigDecimal("0.15")); // 15%止盈

        // 自定义仓位管理
        config.setEnablePositionSize(true);
        config.setMaxPositionRatio(new BigDecimal("0.20")); // 20%最大仓位

        // 自定义技术指标参数
//        config.addParameter("fastPeriod", 8);
//        config.addParameter("slowPeriod", 21);
//        config.addParameter("signalPeriod", 5);

        Strategy strategy = factory.createStrategy(config);

        log.info("自定义策略创建成功: {}", strategy.getName());
        log.info("配置参数: stopLoss={}%, takeProfit={}%, maxPosition={}%",
                config.getStopLossRatio().multiply(new BigDecimal("100")),
                config.getTakeProfitRatio().multiply(new BigDecimal("100")),
                config.getMaxPositionRatio().multiply(new BigDecimal("100")));
    }
}

