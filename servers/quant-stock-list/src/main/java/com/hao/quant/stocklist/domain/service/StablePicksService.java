package com.hao.quant.stocklist.domain.service;

import com.hao.quant.stocklist.application.dto.StablePicksQueryDTO;
import com.hao.quant.stocklist.application.vo.StablePicksVO;
import com.hao.quant.stocklist.common.dto.PageResult;

import java.time.LocalDate;
import java.util.List;

/**
 * 每日精选股票领域服务。
 */
public interface StablePicksService {

    PageResult<StablePicksVO> queryDailyPicks(StablePicksQueryDTO queryDTO);

    List<StablePicksVO> queryLatestPicks(String strategyId, Integer limit);

    StablePicksVO queryStockDetail(String stockCode, LocalDate tradeDate);

    void manualRefreshCache(LocalDate tradeDate, String strategyId);

    void warmupCache(LocalDate tradeDate);
}
