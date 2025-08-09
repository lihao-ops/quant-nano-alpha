package com.hao.datacollector.dal.dao;

import com.hao.datacollector.dto.param.news.NewsQueryParam;
import com.hao.datacollector.web.vo.news.NewsInfoVO;
import com.hao.datacollector.web.vo.news.NewsQueryResultVO;
import org.apache.ibatis.annotations.Param;

import java.util.List;

/**
 * @author hli
 * @Date 2025-06-20 16:10:47
 * @description:
 */
public interface NewsMapper {
    /**
     * 新增新闻base信息
     *
     * @param newInfoVOList 新闻信息对象List
     * @return 操作行数
     */
    int insertNewsInfo(@Param("newInfoVOList") List<NewsInfoVO> newInfoVOList);

    /**
     * 新增股票新闻映射信息
     *
     * @param newsIdList 新闻idList
     * @param windCode   股票代码
     * @return 操作行数
     */
    int insertNewsStockRelation(@Param("newsIdList") List<String> newsIdList, @Param("windCode") String windCode);

    /**
     * 获取近期已转档过新闻信息的windCodeList
     *
     * @return 已转档windCodeList
     */
    List<String> getJobEndWindCodeList();
    
    /**
     * 查询新闻基础数据
     *
     * @param queryParam 查询参数
     * @return 新闻数据列表
     */
    List<NewsQueryResultVO> queryNewsBaseData(@Param("queryParam") NewsQueryParam queryParam);
}