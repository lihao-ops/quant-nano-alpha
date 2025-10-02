package com.hao.strategyengine.chain;

import com.hao.strategyengine.model.Signal;
import com.hao.strategyengine.core.StrategyContext;
import lombok.Data;

/**
 * 风控过滤器接口
 */
public interface RiskFilter {

    /**
     * 设置下一个过滤器
     */
    void setNext(RiskFilter next);

    /**
     * 过滤检查
     *
     * @return FilterResult 过滤结果
     */
    FilterResult filter(StrategyContext context, Signal signal);

    @Data
    class FilterResult {
        private boolean passed;
        private String reason;
        private String filterName;

        public static FilterResult pass(String filterName) {
            FilterResult result = new FilterResult();
            result.setPassed(true);
            result.setFilterName(filterName);
            result.setReason("通过");
            return result;
        }

        public static FilterResult reject(String filterName, String reason) {
            FilterResult result = new FilterResult();
            result.setPassed(false);
            result.setFilterName(filterName);
            result.setReason(reason);
            return result;
        }
    }
}
