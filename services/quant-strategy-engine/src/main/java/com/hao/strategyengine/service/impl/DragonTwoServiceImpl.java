package com.hao.strategyengine.service.impl;

import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.TypeReference;
import com.hao.strategyengine.integration.redis.RedisClient;
import com.hao.strategyengine.service.interf.DragonTwoService;
import constants.RedisKeyConstants;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.HashSet;
import java.util.Map;
import java.util.Set;

/**
 * “龙二战法”策略实现，围绕 Redis 中的涨停与题材映射数据做交叉分析。
 * <p>
 * 通过读取预先计算的涨停股票集合与题材-股票映射，
 * 逐日判断特定题材内涨停占比是否超过阈值，从而输出可能的龙二候选。
 * </p>
 *
 * @author hli
 * @program: quant-nano-alpha
 * @Date 2025-08-26 20:30:17
 * @description:龙二战法
 */
@Slf4j
@Service
public class DragonTwoServiceImpl implements DragonTwoService {

    @Autowired
    private RedisClient<String> redisClient;

    private static final Double HOT_TOPIC_FLAG_NUM = 0.03;

    /**
     * 获取龙二战法信息
     * <p>
     * 1.获取涨停股票列表(key:交易日,value当前交易日涨停股票列表)
     * 2.获取题材库及其映射股票Map<topicId,StockCodeList>
     * 3.遍历涨停股票列表,判断每个交易日中涨停的股票是否在特定题材中占比超过阀值,则表示该题材中该涨停股票有概率被选为龙二
     */
    @Override
    public void getDragonTwoInfo() {
        //获取涨停股票列表
        String limitUpStockListStr = redisClient.get(RedisKeyConstants.DATA_LIMIT_UP_TRADING_DATE_MAPPING_STOCK_MAP);
        //key:交易日期,value:当天涨停股票代码Set
        Map<String, Set<String>> limitUpStockListMap = JSON.parseObject(
                limitUpStockListStr,
                new TypeReference<Map<String, Set<String>>>() {
                }
        );
        //获取题材库及其映射股票Map<topicId,StockCode>
        String topicMappingStockMapStr = redisClient.get(RedisKeyConstants.DATA_TOPIC_MAPPING_STOCK_MAP);
        Map<Integer, Set<String>> topicMappingStockMap = JSON.parseObject(topicMappingStockMapStr,
                new TypeReference<Map<Integer, Set<String>>>() {
                }
        );
        //如果当天涨停股票列表,在某几个题材中占比超过阀值,表示当天这几个题材中有几率选出龙二
        for (Map.Entry<String, Set<String>> entry : limitUpStockListMap.entrySet()) {
            String tradeDate = entry.getKey();
            Set<String> limitUpStockList = entry.getValue();
            for (Map.Entry<Integer, Set<String>> topicEntry : topicMappingStockMap.entrySet()) {
                Integer topicId = topicEntry.getKey();
                Set<String> topicStockList = topicEntry.getValue();
                //获取当天涨停的股票列表在当前题材映射的股票列表中的交集内容intersection
                Set<String> intersection = new HashSet<>(limitUpStockList); // 拷贝一份，不污染原集合
                intersection.retainAll(topicStockList);
                //判断该题材中涨停股票数占比是否超过阀值
                // && intersection.size() / topicStockList.size() > HOT_TOPIC_FLAG_NUM
                if (!intersection.isEmpty()) {
                    // 当前实现仅记录候选题材，后续可在此处增加通知或入库逻辑
                    log.info("tradeDate:{},topicId:{},intersection.size={},topicStockList.size={},intersection:{}", tradeDate, topicId, intersection.size(), topicStockList.size(), intersection);
                }
            }
        }
    }
}
