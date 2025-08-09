package com.hao.datacollector.dal.dao;


import com.hao.datacollector.dto.param.topic.TopicCategoryAndStockParam;
import com.hao.datacollector.dto.param.topic.TopicInfoParam;
import com.hao.datacollector.dto.table.topic.InsertStockCategoryMappingDTO;
import com.hao.datacollector.dto.table.topic.InsertTopicCategoryDTO;
import com.hao.datacollector.dto.table.topic.InsertTopicInfoDTO;
import com.hao.datacollector.dto.table.topic.TopicStockDTO;
import com.hao.datacollector.web.vo.topic.TopicCategoryAndStockVO;
import com.hao.datacollector.web.vo.topic.TopicInfoKplVO;
import org.apache.ibatis.annotations.Param;

import java.util.List;

/**
 * @author Hao Li
 * @program: DataShareService
 * @description: 主题相关mapper
 */
public interface TopicMapper {
    /**
     * 批量插入主题信息
     *
     * @param insertTopicInfoList 主题信息列表
     * @return 插入成功的记录数
     */
    int insertTopicInfoList(@Param("insertList") List<InsertTopicInfoDTO> insertTopicInfoList);

    /**
     * 批量插入主题类别信息
     *
     * @param insertCategoryList 主题类别信息列表
     * @return 插入成功的记录数
     */
    int insertCategoryList(@Param("insertList") List<InsertTopicCategoryDTO> insertCategoryList);

    /**
     * 批量插入股票类别映射信息
     *
     * @param insertStockCategoryMappingList 股票类别映射信息列表
     * @return 插入成功的记录数
     */
    int insertStockCategoryMappingList(@Param("insertList") List<InsertStockCategoryMappingDTO> insertStockCategoryMappingList);

    /**
     * 获取热门题材信息列表
     *
     * @param queryDTO 题材信息查询参数对象，包含分页、筛选、排序等条件
     * @return 题材信息列表
     */
    List<TopicInfoKplVO> getKplTopicInfoList(@Param("param") TopicInfoParam queryDTO);

    /**
     * 获取最大topicId
     *
     * @return topicId
     */
    Integer getKplTopicMaxId();

    /**
     * 获取题材分类及股票映射列表
     *
     * @param queryDTO 题材分类及股票映射查询参数对象，包含分页、筛选、排序等条件
     * @return 题材分类及股票映射列表
     */
    List<TopicCategoryAndStockVO> getKplCategoryAndStockList(@Param("param") TopicCategoryAndStockParam queryDTO);

    /**
     * 获取所有题材idList
     *
     * @return idList
     */
    List<Integer> getKplAllTopicIdList();

    /**
     * 获取题材及其映射股票列表
     *
     * @param topicIdList 题材id列表
     * @return 题材及其映射股票列表
     */
    List<TopicStockDTO> getKplTopicAndStockList(@Param("topicIdList") List<Integer> topicIdList);
}