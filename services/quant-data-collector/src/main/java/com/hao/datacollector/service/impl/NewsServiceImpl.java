package com.hao.datacollector.service.impl;

import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONArray;
import com.hao.datacollector.common.constant.CommonConstants;
import com.hao.datacollector.common.constant.DataSourceConstants;
import com.hao.datacollector.common.utils.HttpUtil;
import com.hao.datacollector.common.utils.PageUtil;
import com.hao.datacollector.dal.dao.BaseDataMapper;
import com.hao.datacollector.dal.dao.NewsMapper;
import com.hao.datacollector.dto.param.news.NewsQueryParam;
import com.hao.datacollector.dto.param.news.NewsRequestParams;
import com.hao.datacollector.service.NewsService;
import com.hao.datacollector.web.vo.news.NewsInfoVO;
import com.hao.datacollector.web.vo.news.NewsQueryResultVO;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.util.HashMap;
import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;

/**
 * @author hli
 * @program: datacollector
 * @Date 2025-06-20 17:10:56
 * @description: 新闻相关实现类
 */
@Slf4j
@Service
public class NewsServiceImpl implements NewsService {

    @Autowired
    private NewsMapper newsMapper;

    @Autowired
    private BaseDataMapper baseDataMapper;

    @Value("${wind_base.session_id}")
    private String windSessionId;

    @Value("${wind_base.news.stock_news_url}")
    private String stockNewsUrl;

    /**
     * 转档股票新闻数据
     *
     * @param windCode 股票代码
     * @return 操作结果
     */
    @Override
    public Boolean transferNewsStockData(String windCode) {
        String url = DataSourceConstants.WIND_PROD_WGQ + stockNewsUrl;
        HashMap<String, String> header = new HashMap<>(4);
        header.put(DataSourceConstants.WIND_POINT_SESSION_NAME, windSessionId);
        NewsRequestParams params = new NewsRequestParams();
        params.setWindCode(windCode);
        // 发送请求，设置超时时间
        String bodyStr = HttpUtil.sendPostRequestTimeOut(url, JSON.toJSONString(params), 10000, header);
        JSONArray jsonArray = JSON.parseArray(bodyStr);
        if (jsonArray == null || !CommonConstants.successCode.equals(jsonArray.get(0))) {
            log.error("NewsServiceImpl_transferNewsStockData_error=windCode={}", windCode);
            throw new RuntimeException("数据异常");
        }
        JSONArray newsArray = JSON.parseArray(jsonArray.getJSONObject(3).getString("value")).getJSONObject(0).getJSONArray("news");
        List<NewsInfoVO> newInfoVOList = JSON.parseArray(newsArray.toJSONString(), NewsInfoVO.class);
        if (newInfoVOList.isEmpty()) {
            boolean insertAbnormalResult = baseDataMapper.insertAbnormalStock(windCode);
            log.error("transferNewsStockData_error,windCode={},insertAbnormalResult={}", windCode, insertAbnormalResult);
            return false;
        }
        int newsInfoResultCount = newsMapper.insertNewsInfo(newInfoVOList);
        List<String> newsIdList = newInfoVOList.stream()
                .map(NewsInfoVO::getId)
                .filter(Objects::nonNull)
                .collect(Collectors.toList());
        int relationResultCount = newsMapper.insertNewsStockRelation(newsIdList, params.getWindCode());
        log.info("NewsServiceImpl_transferNewsStockData_result=newsInfoResultCount={}_relationResultCount={}", newsInfoResultCount, relationResultCount);
        return newsInfoResultCount >= 0;
    }

    /**
     * 查询新闻基础数据
     *
     * @param queryParam 查询参数
     * @return 新闻数据列表
     */
    @Override
    public List<NewsQueryResultVO> queryNewsBaseData(NewsQueryParam queryParam) {
        log.info("NewsServiceImpl_queryNewsBaseData_start=queryParam={}", JSON.toJSONString(queryParam));
        // 处理分页参数，将pageNo转换为offset
        if (queryParam.getPageNo() != null && queryParam.getPageSize() != null) {
            int offset = PageUtil.calculateOffset(queryParam.getPageNo(), queryParam.getPageSize());
            queryParam.setPageNo(offset);
        }
        List<NewsQueryResultVO> result = newsMapper.queryNewsBaseData(queryParam);
        log.info("NewsServiceImpl_queryNewsBaseData_success=result_count={}", result.size());
        return result;
    }
}
