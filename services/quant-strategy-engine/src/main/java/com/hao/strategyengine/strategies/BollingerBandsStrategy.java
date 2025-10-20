package com.hao.strategyengine.strategies;

import com.hao.strategyengine.core.StrategyContext;
import com.hao.strategyengine.model.Signal;
import com.hao.strategyengine.template.AbstractStrategy;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.LinkedList;
import java.util.Queue;

/**
 * 布林带策略（Bollinger Bands）
 * 使用N期均值与标准差构建上下轨，突破上轨做多、跌破下轨做空（动量型）
 */
@Slf4j
@Component
public class BollingerBandsStrategy extends AbstractStrategy {

    private static final String PERIOD = "bollPeriod";        // 计算周期
    private static final String MULTIPLIER = "bollK";         // 标准差倍数

    private final Queue<BigDecimal> window;
    private BigDecimal sma;    // 简单移动平均
    private BigDecimal std;    // 标准差
    private BigDecimal upper;  // 上轨
    private BigDecimal lower;  // 下轨

    public BollingerBandsStrategy() {
        super("BOLLINGER_BANDS_STRATEGY");
        this.window = new LinkedList<>();
    }

    @Override
    public void initialize(StrategyContext context) {
        // 默认参数：20期、2倍标准差
        context.setParameter(PERIOD, 20);
        context.setParameter(MULTIPLIER, new BigDecimal("2"));
        this.ready = true;
        log.info("BOLL策略初始化完成: period={}, k={}", context.getParameter(PERIOD), context.getParameter(MULTIPLIER));
    }

    @Override
    protected void calculateIndicators(StrategyContext context) {
        BigDecimal price = context.getCurrentData().getPrice();
        int period = (Integer) context.getParameter(PERIOD);
        BigDecimal k = (BigDecimal) context.getParameter(MULTIPLIER);

        window.offer(price);
        if (window.size() > period) {
            window.poll();
        }

        // 数据不足时不计算
        if (window.size() < period) {
            sma = null; std = null; upper = null; lower = null;
            context.getState().put("boll.ready", false);
            return;
        }

        // 计算SMA
        BigDecimal sum = BigDecimal.ZERO;
        for (BigDecimal p : window) {
            sum = sum.add(p);
        }
        sma = sum.divide(BigDecimal.valueOf(window.size()), 6, RoundingMode.HALF_UP);

        // 计算标准差
        BigDecimal varianceSum = BigDecimal.ZERO;
        for (BigDecimal p : window) {
            BigDecimal diff = p.subtract(sma);
            varianceSum = varianceSum.add(diff.multiply(diff));
        }
        BigDecimal variance = varianceSum.divide(BigDecimal.valueOf(window.size()), 6, RoundingMode.HALF_UP);
        std = sqrt(variance);

        // 上下轨
        upper = sma.add(std.multiply(k));
        lower = sma.subtract(std.multiply(k));

        // 保存到上下文状态
        context.getState().put("boll.sma", sma);
        context.getState().put("boll.std", std);
        context.getState().put("boll.upper", upper);
        context.getState().put("boll.lower", lower);
        context.getState().put("boll.ready", true);
        context.getState().put("price", price);
    }

    @Override
    protected Signal generateSignal(StrategyContext context) {
        // 数据不足
        Boolean readyFlag = (Boolean) context.getState().get("boll.ready");
        if (readyFlag == null || !readyFlag) {
            return Signal.hold(context.getSymbol());
        }

        BigDecimal price = (BigDecimal) context.getState().get("price");
        BigDecimal u = (BigDecimal) context.getState().get("boll.upper");
        BigDecimal l = (BigDecimal) context.getState().get("boll.lower");

        Signal signal = new Signal();
        signal.setSymbol(context.getSymbol());
        signal.setPrice(price);

        // 动量型：价格突破上轨买入，跌破下轨卖出
        if (price.compareTo(u) > 0) {
            signal.setType(Signal.SignalType.BUY);
            signal.setConfidence(confidenceFromDistance(price, u));
            signal.setReason(String.format("价格突破上轨：price=%.4f > upper=%.4f", price, u));
        } else if (price.compareTo(l) < 0) {
            signal.setType(Signal.SignalType.SELL);
            signal.setConfidence(confidenceFromDistance(l, price));
            signal.setReason(String.format("价格跌破下轨：price=%.4f < lower=%.4f", price, l));
        } else {
            signal.setType(Signal.SignalType.HOLD);
            signal.setConfidence(50);
            signal.setReason("价格位于布林带内部，保持观望");
        }

        return signal;
    }

    private int confidenceFromDistance(BigDecimal a, BigDecimal b) {
        BigDecimal diff = a.subtract(b).abs();
        BigDecimal base = (sma != null && sma.compareTo(BigDecimal.ZERO) > 0) ? sma : BigDecimal.ONE;
        BigDecimal ratio = diff.divide(base, 4, RoundingMode.HALF_UP);
        int score = ratio.multiply(BigDecimal.valueOf(100)).intValue();
        // 限制在[60, 95]之间，避免过低/过高
        return Math.max(60, Math.min(95, score));
    }

    // BigDecimal开平方（牛顿迭代，简化版）
    private BigDecimal sqrt(BigDecimal x) {
        if (x.compareTo(BigDecimal.ZERO) <= 0) return BigDecimal.ZERO;
        BigDecimal g = x; // 初始猜测
        for (int i = 0; i < 20; i++) {
            g = g.add(x.divide(g, 10, RoundingMode.HALF_UP)).divide(BigDecimal.valueOf(2), 10, RoundingMode.HALF_UP);
        }
        return g.setScale(6, RoundingMode.HALF_UP);
    }
}