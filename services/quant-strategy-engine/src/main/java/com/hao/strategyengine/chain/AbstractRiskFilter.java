package com.hao.strategyengine.chain;

import com.hao.strategyengine.model.Signal;
import com.hao.strategyengine.core.StrategyContext;
import lombok.extern.slf4j.Slf4j;

/**
 * 抽象风控过滤器
 */
@Slf4j
public abstract class AbstractRiskFilter implements RiskFilter {
    
    protected RiskFilter next;
    
    @Override
    public void setNext(RiskFilter next) {
        this.next = next;
    }
    
    protected FilterResult passToNext(StrategyContext context, Signal signal) {
        if (next != null) {
            return next.filter(context, signal);
        }
        return FilterResult.pass("CHAIN_END");
    }
}
