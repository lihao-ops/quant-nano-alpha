package com.hao.strategyengine.chain;

import com.hao.strategyengine.model.Signal;
import com.hao.strategyengine.core.StrategyContext;
import lombok.extern.slf4j.Slf4j;

import java.math.BigDecimal;

/**
 * 资金检查过滤器
 */
@Slf4j
public class FundCheckFilter extends AbstractRiskFilter {
    
    private BigDecimal minFundRequired;  // 最小资金要求
    
    public FundCheckFilter(BigDecimal minFundRequired) {
        this.minFundRequired = minFundRequired;
    }
    
    @Override
    public FilterResult filter(StrategyContext context, Signal signal) {
        log.debug("执行资金检查过滤器");
        
        // 只检查买入信号
        if (signal.getType() != Signal.SignalType.BUY) {
            return passToNext(context, signal);
        }
        
        BigDecimal availableFund = context.getAvailableFund();
        BigDecimal requiredFund = signal.getPrice().multiply(signal.getQuantity());
        
        // 资金不足
        if (availableFund.compareTo(requiredFund) < 0) {
            String reason = String.format("资金不足: 可用=%.2f, 需要=%.2f", 
                availableFund, requiredFund);
            log.warn(reason);
            return FilterResult.reject("FUND_CHECK", reason);
        }
        
        // 资金低于最小要求
        if (availableFund.compareTo(minFundRequired) < 0) {
            String reason = String.format("资金低于最小要求: 可用=%.2f, 要求=%.2f", 
                availableFund, minFundRequired);
            log.warn(reason);
            return FilterResult.reject("FUND_CHECK", reason);
        }
        
        log.debug("资金检查通过");
        return passToNext(context, signal);
    }
}
