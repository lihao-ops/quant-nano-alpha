package com.hao.strategyengine.strategies;

import com.hao.strategyengine.model.market.MarketData;
import com.hao.strategyengine.model.Signal;
import com.hao.strategyengine.core.Strategy;
import com.hao.strategyengine.core.StrategyContext;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

/**
 * MA策略单元测试
 */
class MaStrategyTest {

    private Strategy strategy;
    private StrategyContext context;

    @BeforeEach
    void setUp() {
        strategy = new MaStrategy();
        context = new StrategyContext();
        context.setSymbol("AAPL");
        context.setAvailableFund(new BigDecimal("100000"));

        strategy.initialize(context);
    }

    @Test
    void testInitialize() {
        assertTrue(strategy.isReady());
        assertEquals("MA_STRATEGY", strategy.getName());
    }

    @Test
    void testGoldenCross() {
        // 准备测试数据：短期均线上穿长期均线
        List<MarketData> historicalData = generateGoldenCrossData();
        context.setHistoricalData(historicalData);
        context.setCurrentData(historicalData.get(historicalData.size() - 1));

        Signal signal = strategy.analyze(context);

        assertEquals(Signal.SignalType.BUY, signal.getType());
        assertTrue(signal.getConfidence() > 60);
        assertNotNull(signal.getReason());
    }

    @Test
    void testDeathCross() {
        // 准备测试数据：短期均线下穿长期均线
        List<MarketData> historicalData = generateDeathCrossData();
        context.setHistoricalData(historicalData);
        context.setCurrentData(historicalData.get(historicalData.size() - 1));

        Signal signal = strategy.analyze(context);

        assertEquals(Signal.SignalType.SELL, signal.getType());
        assertTrue(signal.getConfidence() > 60);
    }

    @Test
    void testInsufficientData() {
        // 准备不足的数据
        List<MarketData> historicalData = new ArrayList<>();
        for (int i = 0; i < 10; i++) {
            historicalData.add(createMarketData("AAPL", new BigDecimal("150"), i));
        }
        context.setHistoricalData(historicalData);

        assertFalse(strategy.isReady());
    }

    private List<MarketData> generateGoldenCrossData() {
        List<MarketData> data = new ArrayList<>();

        // 生成下降趋势后反转上升的数据（模拟金叉）
        for (int i = 0; i < 60; i++) {
            BigDecimal price;
            if (i < 30) {
                price = new BigDecimal("155").subtract(new BigDecimal(i).multiply(new BigDecimal("1")));
            } else {
                price = new BigDecimal("125").add(new BigDecimal(i - 30).multiply(new BigDecimal("2")));
            }
            data.add(createMarketData("AAPL", price, i));
        }

        return data;
    }

    private List<MarketData> generateDeathCrossData() {
        List<MarketData> data = new ArrayList<>();

        // 生成上升趋势后反转下降的数据（模拟死叉）
        for (int i = 0; i < 60; i++) {
            BigDecimal price;
            if (i < 30) {
                price = new BigDecimal("145").add(new BigDecimal(i).multiply(new BigDecimal("1")));
            } else {
                price = new BigDecimal("175").subtract(new BigDecimal(i - 30).multiply(new BigDecimal("2")));
            }
            data.add(createMarketData("AAPL", price, i));
        }

        return data;
    }

    private MarketData createMarketData(String symbol, BigDecimal price, int daysAgo) {
        MarketData data = new MarketData();
        data.setSymbol(symbol);
        data.setPrice(price);
        data.setVolume(new BigDecimal(1000000L));
        data.setTimestamp(LocalDateTime.now().minusDays(daysAgo));
        return data;
    }
}