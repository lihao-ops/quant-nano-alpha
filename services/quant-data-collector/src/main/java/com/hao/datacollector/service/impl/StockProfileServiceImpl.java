package com.hao.datacollector.service.impl;

import com.alibaba.fastjson.JSONObject;
import com.alibaba.fastjson.TypeReference;
import com.alibaba.fastjson.parser.Feature;
import com.hao.datacollector.common.utils.HttpUtil;
import com.hao.datacollector.properties.DataCollectorProperties;
import com.hao.datacollector.service.StockProfileService;
import com.hao.datacollector.web.vo.stockProfile.SearchKeyBoardVO;
import constants.DataSourceConstants;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpHeaders;
import org.springframework.stereotype.Service;

import java.util.List;


/**
 * 个股资料查询实现，负责代理调用 Wind 键盘精灵接口并反序列化为业务 VO。
 * <p>
 * 通过组合配置化 URL 与统一的 {@link HttpUtil} 客户端来发起请求，
 * 然后使用 Fastjson 将 JSON 数组转换成前端可直接消费的结构。
 * </p>
 *
 * @author hli
 * @program: datacollector
 * @Date 2025-07-22 19:15:53
 * @description: 个股资料相关实现类
 */
@Slf4j
@Service
public class StockProfileServiceImpl implements StockProfileService {

    @Value("${wind_base.keyword.url}")
    private String keywordUrl;

    @Autowired
    private DataCollectorProperties properties;

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
        headers.set(DataSourceConstants.WIND_SESSION_NAME, properties.getWindSessionId());
        // 将配置中的相对地址与生产域名拼接，保持环境切换灵活
        String response = HttpUtil.sendGetRequest(DataSourceConstants.WIND_PROD_WGQ + url, headers, 10000, 30000).getBody();
        // Wind 返回 JSON 数组，直接映射为搜索结果列表
        return JSONObject.parseObject(response, new TypeReference<List<SearchKeyBoardVO>>() {
        }, Feature.OrderedField);
    }
}
