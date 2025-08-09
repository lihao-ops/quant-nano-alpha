package com.hao.datacollector.dal.dao;


import com.hao.datacollector.web.vo.announcement.AnnouncementVO;
import com.hao.datacollector.web.vo.announcement.BigEventVO;
import org.apache.ibatis.annotations.Param;

import java.util.List;

/**
 * @author Hao Li
 * @Date 2025-06-23 14:32:45
 * @description: 公告相关mapper
 */
public interface AnnouncementMapper {

    /**
     * 插入个股公告相关数据
     *
     * @param announcementSourceList 公告List
     * @param windCode               股票代码
     * @return 影响行数
     */
    int insertAnnouncementSourceData(@Param("item") List<AnnouncementVO> announcementSourceList, @Param("windCode") String windCode);

    /**
     * 插入个股大事相关数据
     *
     * @param eventSourceList 大事List
     * @param windCode        股票代码
     * @return 影响行数
     */
    int insertEventSource(@Param("item") List<BigEventVO> eventSourceList, @Param("windCode") String windCode);

    /**
     * 获取已转档公告数据的股票列表
     *
     * @return 已转档股票列表
     */
    List<String> getJobAnnouncementEndWindCodeList(@Param("startDate") String startDate, @Param("endDate") String endDate);

    /**
     * 获取已转档公告数据的最后时间
     * yyyyMMdd格式！
     *
     * @return 已转档股票列表
     */
    String getJobAnnouncementEndLastDate();

    /**
     * 获取已转档大事数据的股票列表
     *
     * @return 已转档股票列表
     */
    List<String> getJobEventEndWindCodeList(@Param("startDate") String startDate, @Param("endDate") String endDate);

    /**
     * 获取已转档公告数据的最后时间
     * yyyyMMdd格式！
     *
     * @return 已转档股票列表
     */
    String getJobEventEndLastDate();
}