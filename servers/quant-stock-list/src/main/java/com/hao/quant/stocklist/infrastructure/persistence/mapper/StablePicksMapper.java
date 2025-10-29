package com.hao.quant.stocklist.infrastructure.persistence.mapper;

import com.hao.quant.stocklist.infrastructure.persistence.po.StrategyDailyPickPO;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.time.LocalDate;
import java.util.List;

/**
 * MyBatis Mapper。
 * <p>
 * 提供基础 SQL 映射,供仓储层调用。
 * </p>
 */
@Mapper
public interface StablePicksMapper {

    /** 查询每日精选列表。 */
    List<StrategyDailyPickPO> queryDaily(@Param("tradeDate") LocalDate tradeDate,
                                         @Param("strategyId") String strategyId,
                                         @Param("industry") String industry,
                                         @Param("offset") int offset,
                                         @Param("limit") int limit);

    /** 统计每日精选列表条数。 */
    long countDaily(@Param("tradeDate") LocalDate tradeDate,
                    @Param("strategyId") String strategyId,
                    @Param("industry") String industry);

    /** 查询最新交易日记录。 */
    List<StrategyDailyPickPO> queryLatest(@Param("strategyId") String strategyId,
                                          @Param("limit") int limit);

    /** 查询股票详情。 */
    StrategyDailyPickPO findDetail(@Param("stockCode") String stockCode,
                                   @Param("tradeDate") LocalDate tradeDate);

    /** 批量插入。 */
    int batchInsert(@Param("records") List<StrategyDailyPickPO> records);

    /** 单条插入。 */
    int insert(StrategyDailyPickPO record);

    /** 查询最近的交易日。 */
    List<LocalDate> listRecentTradeDates(@Param("limit") int limit);
}
