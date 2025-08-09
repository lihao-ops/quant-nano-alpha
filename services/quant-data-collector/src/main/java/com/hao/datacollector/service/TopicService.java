package com.hao.datacollector.service;


import com.hao.datacollector.dto.param.topic.TopicCategoryAndStockParam;
import com.hao.datacollector.dto.param.topic.TopicInfoParam;
import com.hao.datacollector.dto.param.topic.TopicStockQueryParam;
import com.hao.datacollector.dto.table.topic.TopicStockDTO;
import com.hao.datacollector.web.vo.topic.TopicCategoryAndStockVO;
import com.hao.datacollector.web.vo.topic.TopicInfoKplVO;

import java.util.List;
import java.util.Map;

/**
 * @author Hao Li
 * @Date 2025-07-22 10:49:42
 * @description: 题材service
 */
public interface TopicService {
    /**
     * 转档题材库
     *
     * @param startId 遍历题材起始id
     * @param endId   遍历题材结束id
     * @return 转档结果
     */
    Boolean setKplTopicInfoJob(Integer startId, Integer endId);

    /**
     * 获取热门题材信息列表
     *
     * @param queryDTO 题材信息查询参数对象，包含分页、筛选、排序等条件
     * @return 题材信息列表
     */
    List<TopicInfoKplVO> getKplTopicInfoList(TopicInfoParam queryDTO);

    /**
     * 获取题材分类及股票映射列表
     *
     * @param queryDTO 题材分类及股票映射查询参数对象，包含分页、筛选、排序等条件
     * @return 题材分类及股票映射列表
     */
    List<TopicCategoryAndStockVO> getKplCategoryAndStockList(TopicCategoryAndStockParam queryDTO);

    /**
     * 根据id获取题材Id映射股票列表
     *
     * @param query 题材Id映射股票查询参数对象
     * @return 题材Id映射股票对象列表
     */
    Map<Integer, List<TopicStockDTO>> getKplTopicAndStockList(TopicStockQueryParam query);
}