package com.hao.datacollector.service;

import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.TypeReference;
import com.hao.datacollector.common.cache.DateCache;
import com.hao.datacollector.common.cache.TopicCache;
import com.hao.datacollector.common.constant.DateTimeFormatConstants;
import com.hao.datacollector.common.constant.RedisKeyConstants;
import com.hao.datacollector.common.utils.DateUtil;
import com.hao.datacollector.integration.redis.RedisClient;
import lombok.extern.slf4j.Slf4j;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import java.util.List;
import java.util.Map;
import java.util.Set;

@Slf4j
@SpringBootTest
class LimitUpServiceTest {

    @Autowired
    private RedisClient<String> redisClient;

    @Autowired
    private LimitUpService limitUpService;

    @Test
    void transferLimitUpDataToDatabase() {

        List<String> yearTradeDateList = DateUtil.formatLocalDateList(DateCache.CurrentYearTradeDateList, DateTimeFormatConstants.EIGHT_DIGIT_DATE_FORMAT);
        yearTradeDateList.forEach(date -> {
            Boolean success = limitUpService.transferLimitUpDataToDatabase(date);
            if (!success) {
                log.error("Date={} 转档失败", date);
            }
        });
    }

    @Test
    void getLimitUpByTopic() {
        String limitUpTradingDateMappingStockMapStr = redisClient.get(RedisKeyConstants.DATA_LIMIT_UP_TRADING_DATE_MAPPING_STOCK_MAP);
        Map<String, Set<String>> limitUpMappingStockMap = JSON.parseObject(limitUpTradingDateMappingStockMapStr, new TypeReference<Map<String, Set<String>>>() {
        });
        for (Map.Entry<String, Set<String>> limit : limitUpMappingStockMap.entrySet()) {
            Set<String> limitCodeByDate = limit.getValue();
            for (Map.Entry<Integer, Set<String>> topicMappingStockMap : TopicCache.topicMappingStockMap.entrySet()) {
                int containNum = 0;
                for (String windCode : topicMappingStockMap.getValue()) {
                    if (limitCodeByDate.contains(windCode)) {
                        containNum += 1;
                    }
                }
                int total = topicMappingStockMap.getValue().size();
                double percent = total == 0 ? 0.0 : (containNum * 100.0) / total;
                if (percent >= 30) {
                    log.info("limit_Date={},topicId={},containNum={},percent={}", limit.getKey(), topicMappingStockMap.getKey(), containNum, percent);
                }
            }
        }
    }
}