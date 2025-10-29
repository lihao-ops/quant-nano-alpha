package com.hao.quant.stocklist.domain.bloom;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Component;

import java.time.LocalDate;
import java.util.Collection;

/**
 * 布隆过滤器封装,基于 Redis BitMap 实现。
 * <p>
 * 通过 Redis BitMap 记录出现过的交易日,在查询前做快速存在性校验,降低非法入参导致的穿透请求。
 * </p>
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class StablePicksBloomFilter {

    private static final String TRADE_DATE_KEY = "stable:picks:bf:trade-date";

    private final RedisTemplate<String, Object> redisTemplate;

    /**
     * 校验交易日是否可能存在。
     *
     * @param tradeDate 交易日
     * @return true 表示可能存在, false 表示一定不存在
     */
    public boolean mightContainTradeDate(LocalDate tradeDate) {
        long offset = tradeDate.toEpochDay();
        Boolean bit = redisTemplate.opsForValue().getBit(TRADE_DATE_KEY, offset);
        return Boolean.TRUE.equals(bit);
    }

    /**
     * 将交易日写入布隆过滤器。
     *
     * @param tradeDate 交易日
     */
    public void addTradeDate(LocalDate tradeDate) {
        long offset = tradeDate.toEpochDay();
        redisTemplate.opsForValue().setBit(TRADE_DATE_KEY, offset, true);
    }

    /**
     * 批量写入交易日。
     *
     * @param tradeDates 交易日集合
     */
    public void addTradeDates(Collection<LocalDate> tradeDates) {
        tradeDates.forEach(this::addTradeDate);
    }
}
