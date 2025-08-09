package com.hao.datacollector.dal.dao;

import com.hao.datacollector.web.vo.abnormal.AbnormalIndexVO;
import com.hao.datacollector.web.vo.abnormal.ActiveRankRecordVO;
import com.hao.datacollector.web.vo.abnormal.ActiveSeatsRankVO;
import org.apache.ibatis.annotations.Param;

import java.util.List;

/**
 * @author Hao Li
 * @Date 2025-06-23 14:32:45
 * @description: 龙虎榜mapper
 */
public interface AbnormalMapper {
    /**
     * 转档首页数据源
     *
     * @param indexVOList 首页数据源参数对象
     * @param tradeDate   交易日期
     * @return 操作结果
     */
    int insertHomePageSourceData(@Param("item") List<AbnormalIndexVO> indexVOList, @Param("tradeDate") String tradeDate);


    /**
     * 转档龙虎榜席位榜数据源
     *
     * @param sourceListOfSeatList 席位榜
     * @return 操作结果
     */
    int insertSourceListOfSeats(@Param("item") List<ActiveSeatsRankVO> sourceListOfSeatList);

    /**
     * 转档活跃榜数据源
     *
     * @param activeRankVOList 活跃榜数据源
     * @return 操作结果
     */
    int insertActiveRankVOList(@Param("item") List<ActiveRankRecordVO> activeRankVOList);
}