package com.hao.quant.stocklist.domain.event;

import lombok.Builder;
import lombok.Value;

import java.io.Serial;
import java.io.Serializable;
import java.time.LocalDate;
import java.time.LocalDateTime;

/**
 * 策略计算完成事件。
 */
@Value
@Builder
public class StrategyResultEvent implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    String eventId;
    LocalDate tradeDate;
    String strategyId;
    Integer version;
    Integer stockCount;
    LocalDateTime eventTime;
    String source;
}
