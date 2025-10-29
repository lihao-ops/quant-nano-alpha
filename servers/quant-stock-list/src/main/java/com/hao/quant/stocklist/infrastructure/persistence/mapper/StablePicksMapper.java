package com.hao.quant.stocklist.infrastructure.persistence.mapper;

import com.hao.quant.stocklist.infrastructure.persistence.po.StrategyDailyPickPO;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.time.LocalDate;
import java.util.List;

/**
 * MyBatis Mapper。
 */
@Mapper
public interface StablePicksMapper {

    List<StrategyDailyPickPO> queryDaily(@Param("tradeDate") LocalDate tradeDate,
                                         @Param("strategyId") String strategyId,
                                         @Param("industry") String industry,
                                         @Param("offset") int offset,
                                         @Param("limit") int limit);

    long countDaily(@Param("tradeDate") LocalDate tradeDate,
                    @Param("strategyId") String strategyId,
                    @Param("industry") String industry);

    List<StrategyDailyPickPO> queryLatest(@Param("strategyId") String strategyId,
                                          @Param("limit") int limit);

    StrategyDailyPickPO findDetail(@Param("stockCode") String stockCode,
                                   @Param("tradeDate") LocalDate tradeDate);

    int batchInsert(@Param("records") List<StrategyDailyPickPO> records);

    int insert(StrategyDailyPickPO record);

    List<LocalDate> listRecentTradeDates(@Param("limit") int limit);
}
