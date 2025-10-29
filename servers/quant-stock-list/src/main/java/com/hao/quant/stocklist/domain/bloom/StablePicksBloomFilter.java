package com.hao.quant.stocklist.domain.bloom;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Component;

import java.time.LocalDate;
import java.util.Collection;

/**
 * 布隆过滤器封装,基于 Redis BitMap 实现。
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class StablePicksBloomFilter {

    private static final String TRADE_DATE_KEY = "stable:picks:bf:trade-date";

    private final RedisTemplate<String, Object> redisTemplate;

    public boolean mightContainTradeDate(LocalDate tradeDate) {
        long offset = tradeDate.toEpochDay();
        Boolean bit = redisTemplate.opsForValue().getBit(TRADE_DATE_KEY, offset);
        return Boolean.TRUE.equals(bit);
    }

    public void addTradeDate(LocalDate tradeDate) {
        long offset = tradeDate.toEpochDay();
        redisTemplate.opsForValue().setBit(TRADE_DATE_KEY, offset, true);
    }

    public void addTradeDates(Collection<LocalDate> tradeDates) {
        tradeDates.forEach(this::addTradeDate);
    }
}
