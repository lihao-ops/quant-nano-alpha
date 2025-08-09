package com.hao.datacollector.service;

import com.hao.datacollector.dto.param.news.NewsQueryParam;
import com.hao.datacollector.web.vo.news.NewsQueryResultVO;

import java.util.List;

/**
 * @author hli
 * @program: datacollector
 * @Date 2025-06-20 17:09:27
 * @description: 新闻相关service
 */
public interface NewsService {

    /**
     * 转档股票新闻数据
     *
     * @param windCode 股票代码
     * @return 操作结果
     */
    Boolean transferNewsStockData(String windCode);

    /**
     * 查询新闻基础数据
     *
     * @param queryParam 查询参数
     * @return 新闻数据列表
     */
    List<NewsQueryResultVO> queryNewsBaseData(NewsQueryParam queryParam);
}
