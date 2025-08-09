package com.hao.datacollector.service.impl;

import com.alibaba.fastjson.JSONObject;
import com.alibaba.fastjson.TypeReference;
import com.alibaba.fastjson.parser.Feature;
import com.hao.datacollector.common.constant.DataSourceConstants;
import com.hao.datacollector.common.utils.HttpUtil;
import com.hao.datacollector.service.StockProfileService;
import com.hao.datacollector.web.vo.stockProfile.SearchKeyBoardVO;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpHeaders;
import org.springframework.stereotype.Service;

import java.util.List;


/**
 * @author hli
 * @program: datacollector
 * @Date 2025-07-22 19:15:53
 * @description: 个股资料相关实现类
 */
@Slf4j
@Service
public class StockProfileServiceImpl implements StockProfileService {
    @Value("${wind_base.session_id}")
    private String windSessionId;

    @Value("${wind_base.keyword.url}")
    private String keywordUrl;

    /**
     * 获取键盘精灵数据
     *
     * @param keyword  关键词
     * @param pageNo   页号
     * @param pageSize 每页大小
     * @return 匹配内容
     */
    @Override
    public List<SearchKeyBoardVO> getSearchKeyBoard(String keyword, Integer pageNo, Integer pageSize) {
        String url = String.format(keywordUrl, keyword, pageNo, pageSize);
        HttpHeaders headers = new HttpHeaders();
        headers.set(DataSourceConstants.WIND_SESSION_NAME, windSessionId);
        String response = HttpUtil.sendGetRequest(DataSourceConstants.WIND_PROD_WGQ + url, headers, 10000, 30000).getBody();
        return JSONObject.parseObject(response, new TypeReference<List<SearchKeyBoardVO>>() {
        }, Feature.OrderedField);
    }
}
