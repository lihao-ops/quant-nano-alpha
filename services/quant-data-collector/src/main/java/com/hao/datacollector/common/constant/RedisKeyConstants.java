package com.hao.datacollector.common.constant;

/**
 * @author hli
 * @program: datacollector
 * @Date 2025-08-07 17:10:32
 * @description: RedisKey常量类
 * 规范:
 * 所有的key都应遵循 应用名_功能名_业务自定义
 */
public class RedisKeyConstants {

    /**
     * 每个交易日涨停股票代码映射关系
     * data:应用简称
     * limitUp:功能名称
     * tradingDateMappingStockMap:业务自定义
     */
    public static final String DATA_LIMIT_UP_TRADING_DATE_MAPPING_STOCK_MAP = "DATA_LIMIT_UP_TRADING_DATE_MAPPING_STOCK_MAP";
}
