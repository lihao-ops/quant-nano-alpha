package com.hao.quant.stocklist.domain.repository;

import com.hao.quant.stocklist.domain.model.StablePick;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

/**
 * 稳定精选股票仓储接口。
 * <p>
 * 该接口定义了领域模型与底层持久化之间的交互契约,具体实现可基于 MyBatis、JPA 等技术。
 * </p>
 */
public interface StablePicksRepository {

    /**
     * 分页查询指定交易日的精选股票。
     *
     * @param tradeDate 交易日
     * @param strategyId 策略标识
     * @param industry 行业过滤条件
     * @param offset 偏移量
     * @param limit 限制条数
     * @return 股票列表
     */
    List<StablePick> queryDaily(LocalDate tradeDate, String strategyId, String industry, int offset, int limit);

    /**
     * 统计指定条件下的总记录数。
     *
     * @param tradeDate 交易日
     * @param strategyId 策略标识
     * @param industry 行业过滤条件
     * @return 满足条件的记录总数
     */
    long countDaily(LocalDate tradeDate, String strategyId, String industry);

    /**
     * 查询最新交易日的精选股票列表。
     *
     * @param strategyId 策略标识
     * @param limit 限制条数
     * @return 股票列表
     */
    List<StablePick> queryLatest(String strategyId, int limit);

    /**
     * 查找指定股票在特定交易日的详情。
     *
     * @param stockCode 股票代码
     * @param tradeDate 交易日
     * @return 股票详情
     */
    Optional<StablePick> findDetail(String stockCode, LocalDate tradeDate);

    /**
     * 获取最近的交易日列表,用于缓存预热或调度任务。
     *
     * @param limit 返回数量
     * @return 交易日列表
     */
    List<LocalDate> listRecentTradeDates(int limit);
}
