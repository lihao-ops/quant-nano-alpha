package com.hao.quant.stocklist.domain.service;

import com.hao.quant.stocklist.application.dto.StablePicksQueryDTO;
import com.hao.quant.stocklist.application.vo.StablePicksVO;
import com.hao.quant.stocklist.common.dto.PageResult;

import java.time.LocalDate;
import java.util.List;

/**
 * 每日精选股票领域服务。
 * <p>
 * 对外暴露缓存化的股票列表查询能力,并提供缓存刷新、预热等运维接口。
 * </p>
 */
public interface StablePicksService {

    /**
     * 查询指定交易日的分页结果。
     *
     * @param queryDTO 查询条件
     * @return 分页结果
     */
    PageResult<StablePicksVO> queryDailyPicks(StablePicksQueryDTO queryDTO);

    /**
     * 查询最新交易日的股票列表。
     *
     * @param strategyId 策略标识
     * @param limit      返回条数
     * @return 股票列表
     */
    List<StablePicksVO> queryLatestPicks(String strategyId, Integer limit);

    /**
     * 查询单支股票在指定交易日的详情。
     *
     * @param stockCode 股票代码
     * @param tradeDate 交易日
     * @return 股票详情
     */
    StablePicksVO queryStockDetail(String stockCode, LocalDate tradeDate);

    /**
     * 手动刷新指定交易日及策略的数据缓存。
     *
     * @param tradeDate 交易日
     * @param strategyId 策略标识
     */
    void manualRefreshCache(LocalDate tradeDate, String strategyId);

    /**
     * 预热指定交易日的缓存,常用于定时任务。
     *
     * @param tradeDate 交易日
     */
    void warmupCache(LocalDate tradeDate);
}
