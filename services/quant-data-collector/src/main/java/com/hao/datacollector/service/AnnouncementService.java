package com.hao.datacollector.service;


import com.hao.datacollector.web.vo.announcement.AnnouncementVO;
import com.hao.datacollector.web.vo.announcement.BigEventVO;

import java.util.List;

/**
 * @author hli
 * @description 公告页数据接口服务
 */
public interface AnnouncementService {

    /**
     * 个股公告数据源
     *
     * @param windCode  股票代码
     * @param startDate 起始日期
     * @param endDate   结束日期
     * @param pageNo    页号
     * @param pageSize  页面规模
     * @return 个股公告数据
     */
    List<AnnouncementVO> getAnnouncementSourceData(String windCode, String startDate, String endDate, Integer pageNo, Integer pageSize);

    /**
     * 转档公告数据源
     *
     * @param windCode  股票代码
     * @param startDate 起始日期
     * @param endDate   结束日期
     * @param pageNo    页号
     * @param pageSize  页面规模
     * @return 操作结果
     */
    Boolean transferAnnouncement(String windCode, String startDate, String endDate, Integer pageNo, Integer pageSize);

    /**
     * 个股大事数据源
     *
     * @param windCode  股票代码
     * @param startDate 起始日期
     * @param endDate   结束日期
     * @param pageNo    页号
     * @param pageSize  页面规模
     * @return 个股大事数据源
     */
    List<BigEventVO> getEventSourceData(String windCode, String startDate, String endDate, Integer pageNo, Integer pageSize);

    /**
     * 转档大事数据源
     *
     * @param windCode  股票代码
     * @param startDate 起始日期
     * @param endDate   结束日期
     * @param pageNo    页号
     * @param pageSize  页面规模
     * @return 操作结果
     */
    Boolean transferEvent(String windCode, String startDate, String endDate, Integer pageNo, Integer pageSize);
}