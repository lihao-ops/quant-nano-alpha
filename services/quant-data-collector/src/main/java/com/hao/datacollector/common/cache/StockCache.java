package com.hao.datacollector.common.cache;

import com.hao.datacollector.dal.dao.BaseDataMapper;
import com.hao.datacollector.dto.table.base.StockBaseDTO;
import jakarta.annotation.PostConstruct;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * @author hli
 * @program: datacollector
 * @Date 2025-06-20 21:11:07
 * @description: 股票相关缓存
 */
@Slf4j
@Component("StockCache")
public class StockCache {

    @Autowired
    private BaseDataMapper baseDataMapper;

    /**
     * 全部A股代码
     */
    public static List<String> allWindCode;

    /**
     * 股票ID前缀 -> 完整wind_code 映射
     * 例如：000001 -> 000001.SZ
     */
    public static Map<String, String> stockIdToWindCodeMap = new HashMap<>();

    /**
     * 股票基本信息map
     * key:windCode,value:windName
     */
    public static Map<String, String> windCodeToNameMap = new HashMap<>();

    @PostConstruct
    private void initDateList() {
        allWindCode = baseDataMapper.getAllAStockCode();
        log.info("StockCache_allWindCode.size={}", allWindCode.size());
        List<StockBaseDTO> allWindBaseInfo = baseDataMapper.getAllStockBaseInfo();
        windCodeToNameMap = allWindBaseInfo.stream()
                .collect(Collectors.toMap(StockBaseDTO::getWindCode, StockBaseDTO::getWindName));
        log.info("StockCache_allWindBaseInfo.size={},windCodeToNameMap.size={}", allWindBaseInfo.size(), windCodeToNameMap.size());
        for (String windCode : allWindCode) {
            // 例如 windCode 是 000001.SZ，截取前缀作为 key
            String prefix = windCode.split("\\.")[0];
            // 避免覆盖已有的
            stockIdToWindCodeMap.putIfAbsent(prefix, windCode);
        }
        log.info("StockCache_stockIdToWindCodeMap.size={}", stockIdToWindCodeMap.size());
    }

    /**
     * 根据股票ID获取完整wind_code
     */
    public static String getWindCodeByStockId(String stockId) {
        return stockIdToWindCodeMap.get(stockId);
    }

    /**
     * 根据windCode获取股票名称
     */
    public static String getWindNameByWindCode(String windCode) {
        return windCodeToNameMap.get(windCode);
    }
}
