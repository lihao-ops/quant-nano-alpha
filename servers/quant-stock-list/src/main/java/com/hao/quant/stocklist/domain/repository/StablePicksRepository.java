package com.hao.quant.stocklist.domain.repository;

import com.hao.quant.stocklist.domain.model.StablePick;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

/**
 * 稳定精选股票仓储接口。
 */
public interface StablePicksRepository {

    List<StablePick> queryDaily(LocalDate tradeDate, String strategyId, String industry, int offset, int limit);

    long countDaily(LocalDate tradeDate, String strategyId, String industry);

    List<StablePick> queryLatest(String strategyId, int limit);

    Optional<StablePick> findDetail(String stockCode, LocalDate tradeDate);

    List<LocalDate> listRecentTradeDates(int limit);
}
