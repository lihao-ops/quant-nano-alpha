package com.hao.strategyengine.config;


import com.hao.strategyengine.chain.FrequencyLimitFilter;
import com.hao.strategyengine.chain.FundCheckFilter;
import com.hao.strategyengine.chain.PositionLimitFilter;
import com.hao.strategyengine.chain.RiskFilter;
import com.hao.strategyengine.engine.StrategyEngine;
import com.hao.strategyengine.event.listener.LoggingEventListener;
import com.hao.strategyengine.event.listener.MetricsEventListener;
import com.hao.strategyengine.event.listener.StrategyEventPublisher;
import com.hao.strategyengine.factory.StrategyFactory;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.math.BigDecimal;

/**
 * 策略配置类
 */
@Slf4j
@Configuration
public class StrategyConfiguration {
    
    /**
     * 配置风控过滤链
     */
    @Bean
    public RiskFilter riskFilterChain() {
        // 创建过滤器链：资金检查 -> 仓位限制 -> 交易频率
        FundCheckFilter fundCheckFilter = new FundCheckFilter(new BigDecimal("10000"));
        PositionLimitFilter positionLimitFilter = new PositionLimitFilter(
            new BigDecimal("0.30"),  // 单品种最大30%仓位
            5                         // 最多持有5个品种
        );
        FrequencyLimitFilter frequencyLimitFilter = new FrequencyLimitFilter(
            10,      // 每日最多10笔交易
            300      // 冷却时间300秒
        );
        
        // 构建责任链
        fundCheckFilter.setNext(positionLimitFilter);
        positionLimitFilter.setNext(frequencyLimitFilter);
        
        log.info("风控过滤链配置完成");
        return fundCheckFilter;
    }
    
    /**
     * 配置事件发布器和监听器
     */
    @Bean
    public StrategyEventPublisher strategyEventPublisher(
            LoggingEventListener loggingListener,
            MetricsEventListener metricsListener) {
        
        StrategyEventPublisher publisher = new StrategyEventPublisher();
        publisher.registerListener(loggingListener);
        publisher.registerListener(metricsListener);
        
        log.info("事件发布器配置完成");
        return publisher;
    }
    
    /**
     * 初始化策略引擎
     */
    @Bean
    public StrategyEngine strategyEngine(
            RiskFilter riskFilterChain,
            StrategyFactory strategyFactory) {
        
        StrategyEngine engine = new StrategyEngine(null); // TODO: 注入executor
        engine.setRiskFilterChain(riskFilterChain);
        
        log.info("策略引擎初始化完成");
        return engine;
    }
}
