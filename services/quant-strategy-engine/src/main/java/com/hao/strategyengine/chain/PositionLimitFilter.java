package com.hao.strategyengine.chain;

import com.hao.strategyengine.model.Signal;
import com.hao.strategyengine.core.StrategyContext;
import lombok.extern.slf4j.Slf4j;

import java.math.BigDecimal;

/**
 * 仓位限制过滤器
 */
@Slf4j
public class PositionLimitFilter extends AbstractRiskFilter {
    
    private BigDecimal maxPositionRatio;     // 单品种最大仓位比例
    private Integer maxPositionCount;         // 最大持仓品种数
    
    public PositionLimitFilter(BigDecimal maxPositionRatio, Integer maxPositionCount) {
        this.maxPositionRatio = maxPositionRatio;
        this.maxPositionCount = maxPositionCount;
    }
    
    @Override
    public FilterResult filter(StrategyContext context, Signal signal) {
        log.debug("执行仓位限制过滤器");
        
        if (signal.getType() != Signal.SignalType.BUY) {
            return passToNext(context, signal);
        }
        
        // 检查单品种仓位比例
        BigDecimal totalFund = context.getAvailableFund().add(context.getCurrentPosition());
        BigDecimal requiredFund = signal.getPrice().multiply(signal.getQuantity());
        BigDecimal positionRatio = requiredFund.divide(totalFund, 4, BigDecimal.ROUND_HALF_UP);
        
        if (positionRatio.compareTo(maxPositionRatio) > 0) {
            String reason = String.format("仓位比例超限: 实际=%.2f%%, 限制=%.2f%%", 
                positionRatio.multiply(BigDecimal.valueOf(100)),
                maxPositionRatio.multiply(BigDecimal.valueOf(100)));
            log.warn(reason);
            return FilterResult.reject("POSITION_LIMIT", reason);
        }
        
        // TODO: 检查持仓品种数（需要从持仓服务获取数据）
        
        log.debug("仓位限制检查通过");
        return passToNext(context, signal);
    }
}
