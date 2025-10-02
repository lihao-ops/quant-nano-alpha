package com.hao.strategyengine.composite;

import com.hao.strategyengine.model.Signal;
import com.hao.strategyengine.core.Strategy;
import com.hao.strategyengine.core.StrategyContext;
import lombok.extern.slf4j.Slf4j;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;

/**
 * 组合策略
 * 支持多个策略的信号融合
 */
@Slf4j
public class CompositeStrategy implements Strategy {

    private String name;
    private List<Strategy> strategies;
    private SignalMergeStrategy mergeStrategy;

    public enum SignalMergeStrategy {
        VOTE,           // 投票制（少数服从多数）
        WEIGHTED,       // 加权平均
        ALL_AGREE,      // 全部同意
        ANY_AGREE       // 任一同意
    }

    public CompositeStrategy(String name, SignalMergeStrategy mergeStrategy) {
        this.name = name;
        this.strategies = new ArrayList<>();
        this.mergeStrategy = mergeStrategy;
    }

    public void addStrategy(Strategy strategy) {
        strategies.add(strategy);
        log.info("组合策略[{}]添加子策略: {}", name, strategy.getName());
    }

    @Override
    public String getName() {
        return name;
    }

    @Override
    public void initialize(StrategyContext context) {
        for (Strategy strategy : strategies) {
            strategy.initialize(context);
        }
    }

    @Override
    public boolean isReady() {
        return strategies.stream().allMatch(Strategy::isReady);
    }

    @Override
    public Signal analyze(StrategyContext context) {
        List<Signal> signals = new ArrayList<>();

        // 收集所有子策略的信号
        for (Strategy strategy : strategies) {
            Signal signal = strategy.analyze(context);
            signals.add(signal);
            log.debug("子策略[{}]信号: {}", strategy.getName(), signal.getType());
        }

        // 根据合并策略融合信号
        return mergeSignals(context, signals);
    }

    /**
     * 信号融合
     */
    private Signal mergeSignals(StrategyContext context, List<Signal> signals) {
        switch (mergeStrategy) {
            case VOTE:
                return voteSignals(context, signals);
            case WEIGHTED:
                return weightedSignals(context, signals);
            case ALL_AGREE:
                return allAgreeSignals(context, signals);
            case ANY_AGREE:
                return anyAgreeSignals(context, signals);
            default:
                return Signal.hold(context.getSymbol());
        }
    }

    /**
     * 投票制：少数服从多数
     */
    private Signal voteSignals(StrategyContext context, List<Signal> signals) {
        int buyVotes = 0, sellVotes = 0, holdVotes = 0;

        for (Signal signal : signals) {
            switch (signal.getType()) {
                case BUY: buyVotes++; break;
                case SELL: sellVotes++; break;
                case HOLD: holdVotes++; break;
            }
        }

        Signal result = new Signal();
        result.setSymbol(context.getSymbol());
        result.setPrice(context.getCurrentData().getPrice());

        if (buyVotes > sellVotes && buyVotes > holdVotes) {
            result.setType(Signal.SignalType.BUY);
            result.setConfidence(buyVotes * 100 / signals.size());
            result.setReason(String.format("投票制：买入票数=%d", buyVotes));
        } else if (sellVotes > buyVotes && sellVotes > holdVotes) {
            result.setType(Signal.SignalType.SELL);
            result.setConfidence(sellVotes * 100 / signals.size());
            result.setReason(String.format("投票制：卖出票数=%d", sellVotes));
        } else {
            result.setType(Signal.SignalType.HOLD);
            result.setConfidence(holdVotes * 100 / signals.size());
            result.setReason("投票制：无明确信号");
        }

        log.info("投票结果 - 买入:{}, 卖出:{}, 持有:{}, 最终:{}",
                buyVotes, sellVotes, holdVotes, result.getType());

        return result;
    }

    /**
     * 加权平均：根据信号强度加权
     */
    private Signal weightedSignals(StrategyContext context, List<Signal> signals) {
        BigDecimal buyScore = BigDecimal.ZERO;
        BigDecimal sellScore = BigDecimal.ZERO;
        BigDecimal totalWeight = BigDecimal.ZERO;

        for (Signal signal : signals) {
            BigDecimal weight = BigDecimal.valueOf(signal.getConfidence());
            totalWeight = totalWeight.add(weight);

            if (signal.getType() == Signal.SignalType.BUY) {
                buyScore = buyScore.add(weight);
            } else if (signal.getType() == Signal.SignalType.SELL) {
                sellScore = sellScore.add(weight);
            }
        }

        Signal result = new Signal();
        result.setSymbol(context.getSymbol());
        result.setPrice(context.getCurrentData().getPrice());

        if (buyScore.compareTo(sellScore) > 0) {
            result.setType(Signal.SignalType.BUY);
            result.setConfidence(buyScore.multiply(BigDecimal.valueOf(100))
                    .divide(totalWeight, 0, BigDecimal.ROUND_HALF_UP).intValue());
            result.setReason(String.format("加权平均：买入得分=%.2f", buyScore));
        } else if (sellScore.compareTo(buyScore) > 0) {
            result.setType(Signal.SignalType.SELL);
            result.setConfidence(sellScore.multiply(BigDecimal.valueOf(100))
                    .divide(totalWeight, 0, BigDecimal.ROUND_HALF_UP).intValue());
            result.setReason(String.format("加权平均：卖出得分=%.2f", sellScore));
        } else {
            result.setType(Signal.SignalType.HOLD);
            result.setConfidence(50);
            result.setReason("加权平均：买卖得分相当");
        }

        return result;
    }

    /**
     * 全部同意：所有策略都同意才执行
     */
    private Signal allAgreeSignals(StrategyContext context, List<Signal> signals) {
        Signal.SignalType firstType = signals.get(0).getType();

        boolean allAgree = signals.stream()
                .allMatch(s -> s.getType() == firstType);

        Signal result = new Signal();
        result.setSymbol(context.getSymbol());
        result.setPrice(context.getCurrentData().getPrice());

        if (allAgree && firstType != Signal.SignalType.HOLD) {
            result.setType(firstType);
            result.setConfidence(90);
            result.setReason("全部策略一致同意: " + firstType);
        } else {
            result.setType(Signal.SignalType.HOLD);
            result.setConfidence(50);
            result.setReason("策略意见不一致");
        }

        return result;
    }

    /**
     * 任一同意：只要有一个策略同意就执行
     */
    private Signal anyAgreeSignals(StrategyContext context, List<Signal> signals) {
        for (Signal signal : signals) {
            if (signal.getType() != Signal.SignalType.HOLD) {
                Signal result = new Signal();
                result.setSymbol(context.getSymbol());
                result.setType(signal.getType());
                result.setPrice(signal.getPrice());
                result.setConfidence(signal.getConfidence());
                result.setReason("策略[" + signal.getStrategyName() + "]建议: " + signal.getReason());
                return result;
            }
        }

        return Signal.hold(context.getSymbol());
    }
}

