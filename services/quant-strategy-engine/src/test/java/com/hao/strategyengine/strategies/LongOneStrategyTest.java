package com.hao.strategyengine.strategies;

import com.hao.strategyengine.factory.StrategyConfig;
import com.hao.strategyengine.factory.StrategyFactory;
import com.hao.strategyengine.common.model.market.MarketData;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import java.math.BigDecimal;

@SpringBootTest
public class LongOneStrategyTest {

    @Autowired
    private StrategyFactory strategyFactory;

    @Test
    public void testLongOneBasicStrategy() {
        StrategyConfig config = StrategyConfig.defaultConfig("LongOne");
        Strategy strategy = strategyFactory.createStrategy(config);
        StrategyContext context = new StrategyContext();
        context.setSymbol("AAPL");
        context.setAvailableFund(new BigDecimal("100000"));
        strategy.initialize(context);
        context.setCurrentData(new MarketData());
        if (strategy.isReady()) {
            Signal signal = strategy.analyze(context);
            System.out.println("生成信号: " + signal);
        }
    }
}