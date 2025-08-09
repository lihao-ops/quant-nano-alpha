package com.hao.datacollector.common.cache;

import com.hao.datacollector.dal.dao.TopicMapper;
import com.hao.datacollector.dto.table.topic.TopicStockDTO;
import jakarta.annotation.PostConstruct;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.*;

/**
 * @author hli
 * @program: datacollector
 * @Date 2025-08-03 22:09:00
 * @description: 题材相关缓存
 */
@Slf4j
@Component("TopicCache")
public class TopicCache {
    @Autowired
    private TopicMapper topicMapper;
    /**
     * 题材信息缓存Map
     * key:题材id
     * value:题材所属类别下股票代码列表
     * <p>
     * 思路:
     * 1.先查询tb_topic_info表中所有的题材id List
     * 2.遍历查询所有
     */
    public static Map<Integer, Set<String>> topicMappingStockMap = new HashMap<>();

    @PostConstruct
    public void initTopicMappingStockCache() {
        List<TopicStockDTO> kplTopicAndStockList = topicMapper.getKplTopicAndStockList(null);
        Map<Integer, Set<String>> resultMap = new HashMap<>();
        for (TopicStockDTO dto : kplTopicAndStockList) {
            Integer topicId = dto.getTopicId();
            resultMap
                    .computeIfAbsent(topicId, k -> new HashSet<>())
                    .add(dto.getWindCode());
        }
        topicMappingStockMap = resultMap;
        log.info("TopicCache_allTopicIdList.size={}", kplTopicAndStockList.size());
    }
}
