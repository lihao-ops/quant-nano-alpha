package com.hao.datacollector.dal.dao;

import com.hao.datacollector.dto.param.limitup.LimitUpStockQueryParam;
import com.hao.datacollector.dto.table.limitup.LimitUpStockInfoInsertDTO;
import com.hao.datacollector.dto.table.limitup.LimitUpStockTopicRelationInsertDTO;
import com.hao.datacollector.dto.table.limitup.LimitUpStockTradeDTO;
import com.hao.datacollector.dto.table.topic.BaseTopicInsertDTO;
import com.hao.datacollector.web.vo.limitup.LimitUpStockQueryResultVO;
import org.apache.ibatis.annotations.Param;

import java.util.List;

public interface LimitUpMapper {

    /**
     * 批量插入基础标签信息表(存在即更新)
     *
     * @param baseTopicDTO 基础标签对象
     * @return 影响行数
     */
    Boolean insertBaseTopic(@Param("baseTopicDTO") BaseTopicInsertDTO baseTopicDTO);

    /**
     * 批量插入股票标签关联数据
     *
     * @param relationInsertList 股票标签关联数据列表
     * @return 影响行数
     */
    int batchInsertStockTopicRelation(@Param("list") List<LimitUpStockTopicRelationInsertDTO> relationInsertList);

    /**
     * 批量插入每日涨停股票数据
     *
     * @param limitUpStockDailyList 每日涨停股票明细数据列表
     * @return 影响行数
     */
    int batchInsertLimitUpStockInfo(@Param("list") List<LimitUpStockInfoInsertDTO> limitUpStockDailyList);

    /**
     * 根据交易日删除每日涨停股票明细数据
     *
     * @param tradeDate 交易日
     */
    void deleteLimitUpStockInfoByTradeDate(@Param("tradeDate") String tradeDate);

    /**
     * 根据交易日删除股票标签关联数据
     *
     * @param tradeDate 交易日
     */
    void deleteStockTopicRelationByTradeDate(@Param("tradeDate") String tradeDate);

    /**
     * 查询涨停股票信息列表
     *
     * @param queryParam 查询参数
     * @return 结果列表
     */
    List<LimitUpStockQueryResultVO> queryLimitUpStockList(@Param("queryParam") LimitUpStockQueryParam queryParam);

    /**
     * 查询涨停股票对应交易日映射关系
     *
     * @param tradeDateStart 开始交易日
     * @param tradeDateEnd   结束交易日
     * @return 结果列表
     */
    List<LimitUpStockTradeDTO> getLimitCodeByTradeDate(@Param("tradeDateStart") String tradeDateStart, @Param("tradeDateEnd") String tradeDateEnd);
}
