package com.hao.datacollector.service;

import com.hao.datacollector.web.vo.stockProfile.SearchKeyBoardVO;

import java.util.List;

/**
 * @author hli
 * @program: datacollector
 * @Date 2025-07-22 19:15:53
 * @description: 个股资料相关service
 */
public interface StockProfileService {
    /**
     * 获取键盘精灵数据
     *
     * @param keyword  关键词
     * @param pageNo   页号
     * @param pageSize 每页大小
     * @return 匹配内容
     */
    List<SearchKeyBoardVO> getSearchKeyBoard(String keyword, Integer pageNo, Integer pageSize);
}
