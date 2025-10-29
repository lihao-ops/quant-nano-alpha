package com.hao.quant.stocklist.infrastructure.persistence.repository;

import com.hao.quant.stocklist.domain.model.StablePick;
import com.hao.quant.stocklist.domain.repository.StablePicksRepository;
import com.hao.quant.stocklist.infrastructure.persistence.mapper.StablePicksMapper;
import com.hao.quant.stocklist.infrastructure.persistence.po.StrategyDailyPickPO;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

/**
 * 基于 MyBatis 的仓储实现。
 */
@Repository
@RequiredArgsConstructor
public class MyBatisStablePicksRepository implements StablePicksRepository {

    private final StablePicksMapper stablePicksMapper;

    @Override
    public List<StablePick> queryDaily(java.time.LocalDate tradeDate, String strategyId, String industry, int offset, int limit) {
        List<StrategyDailyPickPO> list = stablePicksMapper.queryDaily(tradeDate, strategyId, industry, offset, limit);
        return list.stream().map(this::convert).collect(Collectors.toList());
    }

    @Override
    public long countDaily(java.time.LocalDate tradeDate, String strategyId, String industry) {
        return stablePicksMapper.countDaily(tradeDate, strategyId, industry);
    }

    @Override
    public List<StablePick> queryLatest(String strategyId, int limit) {
        List<StrategyDailyPickPO> list = stablePicksMapper.queryLatest(strategyId, limit);
        return list.stream().map(this::convert).collect(Collectors.toList());
    }

    @Override
    public Optional<StablePick> findDetail(String stockCode, java.time.LocalDate tradeDate) {
        return Optional.ofNullable(stablePicksMapper.findDetail(stockCode, tradeDate)).map(this::convert);
    }

    @Override
    public List<java.time.LocalDate> listRecentTradeDates(int limit) {
        return stablePicksMapper.listRecentTradeDates(limit);
    }

    private StablePick convert(StrategyDailyPickPO po) {
        return StablePick.builder()
                .strategyId(po.getStrategyId())
                .stockCode(po.getStockCode())
                .stockName(po.getStockName())
                .industry(po.getIndustry())
                .score(po.getScore())
                .ranking(po.getRanking())
                .marketCap(po.getMarketCap())
                .peRatio(po.getPeRatio())
                .tradeDate(po.getTradeDate())
                .extraData(po.getExtraData())
                .build();
    }
}
