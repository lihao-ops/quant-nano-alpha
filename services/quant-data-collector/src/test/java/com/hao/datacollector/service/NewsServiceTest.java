package com.hao.datacollector.service;

import com.hao.datacollector.common.cache.StockCache;
import com.hao.datacollector.dal.dao.BaseDataMapper;
import com.hao.datacollector.dal.dao.NewsMapper;
import lombok.extern.slf4j.Slf4j;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import java.util.List;

@Slf4j
@SpringBootTest
class NewsServiceTest {

    @Autowired
    private NewsService newsService;

    @Autowired
    private NewsMapper newsMapper;

    @Autowired
    private BaseDataMapper baseDataMapper;

    /**
     * 转档全部A股近期新闻列表
     */
    @Test
    void transferNewsStockData() {
        //去除近期已转档过的代码
        List<String> jobStockList = StockCache.allWindCode;
        List<String> jobEndList = newsMapper.getJobEndWindCodeList();
        jobStockList.removeAll(jobEndList);
        //删除异常股票列表
        List<String> abnormalStockList = baseDataMapper.getAbnormalStockList();
        jobStockList.removeAll(abnormalStockList);
        for (String windCode : jobStockList) {
            Boolean transferNewsStockResult = newsService.transferNewsStockData(windCode);
            log.info("NewsServiceTest_transferNewsStockData_windCode={},transferNewsStockResult={}", windCode, transferNewsStockResult);
        }
    }
}