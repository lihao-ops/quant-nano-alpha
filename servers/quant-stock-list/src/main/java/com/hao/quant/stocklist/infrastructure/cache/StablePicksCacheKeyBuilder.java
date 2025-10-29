package com.hao.quant.stocklist.infrastructure.cache;

import com.hao.quant.stocklist.application.dto.StablePicksQueryDTO;
import org.apache.commons.lang3.StringUtils;
import org.springframework.stereotype.Component;

import java.time.format.DateTimeFormatter;

/**
 * 缓存 Key 构建器。
 */
@Component
public class StablePicksCacheKeyBuilder {

    private static final DateTimeFormatter DATE_FORMAT = DateTimeFormatter.ISO_DATE;

    public String buildDailyKey(StablePicksQueryDTO query) {
        StringBuilder builder = new StringBuilder("stable:picks:daily:");
        builder.append(DATE_FORMAT.format(query.getTradeDate()));
        builder.append(":");
        builder.append(StringUtils.defaultString(query.getStrategyId(), "ALL"));
        builder.append(":");
        builder.append(StringUtils.defaultString(query.getIndustry(), "ALL"));
        builder.append(":");
        builder.append(query.getPageNum()).append(":").append(query.getPageSize());
        return builder.toString();
    }

    public String buildLatestKey(String strategyId, Integer limit) {
        return "stable:picks:latest:" + StringUtils.defaultString(strategyId, "ALL") + ":" + limit;
    }

    public String buildDetailKey(String stockCode, String tradeDate) {
        return "stable:picks:detail:" + stockCode + ":" + tradeDate;
    }
}
