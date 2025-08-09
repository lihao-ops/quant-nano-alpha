package com.hao.datacollector.service.impl;

import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.TypeReference;
import com.hao.datacollector.common.constant.DataSourceConstants;
import com.hao.datacollector.common.utils.HttpUtil;
import com.hao.datacollector.dal.dao.AnnouncementMapper;
import com.hao.datacollector.service.AnnouncementService;
import com.hao.datacollector.web.vo.announcement.AnnouncementVO;
import com.hao.datacollector.web.vo.announcement.BigEventVO;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpHeaders;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;

import java.util.List;
import java.util.stream.Collectors;

/**
 * @author hli
 * @Date 2025-06-24 13:52:41
 * @description: 公告数据实现类
 */
@Slf4j
@Service
public class AnnouncementServiceImpl implements AnnouncementService {

    @Autowired
    private AnnouncementMapper announcementMapper;

    @Value("${wind_base.session_id}")
    private String windSessionId;

    @Value("${wind_base.announcement.url}")
    private String AnnouncementUrl;

    @Value("${wind_base.event.url}")
    private String eventUrl;


    private static final String DATE_TIME_SEPARATOR = "T";
    private static final String UTC_FLAG = "Z";

    private static final String SUCCESS_FLAG = "200 OK";

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
    @Override
    public List<AnnouncementVO> getAnnouncementSourceData(String windCode, String startDate, String endDate, Integer pageNo, Integer pageSize) {
        String url = String.format(DataSourceConstants.WIND_PROD_WGQ + AnnouncementUrl, windCode);
        org.springframework.http.HttpHeaders headers = new HttpHeaders();
        headers.set(DataSourceConstants.WIND_POINT_SESSION_NAME, windSessionId);
        MultiValueMap<String, String> queryParams = new LinkedMultiValueMap<>();
        queryParams.add("startDate", startDate);
        queryParams.add("endDate", endDate);
        queryParams.add("pageNo", String.valueOf(pageNo));
        queryParams.add("pageSize", String.valueOf(pageSize));
        ResponseEntity<String> response = HttpUtil.sendGetWithParams(url, queryParams, headers, 100000, 100000);
        if (!SUCCESS_FLAG.equals(response.getStatusCode().toString())) {
            throw new RuntimeException("getBigEventData_error,result=" + response.getStatusCode());
        }
        List<AnnouncementVO> announcementList = JSON.parseObject(response.getBody(), new TypeReference<List<AnnouncementVO>>() {
        });
        return announcementList;
    }

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
    @Override
    public Boolean transferAnnouncement(String windCode, String startDate, String endDate, Integer pageNo, Integer pageSize) {
        List<AnnouncementVO> announcementVOList = getAnnouncementSourceData(windCode, startDate, endDate, pageNo, pageSize);
        if (announcementVOList.isEmpty()) {
            log.error("transferAnnouncement_announcementVOList.isEmpty()!windCode={}", windCode);
            return false;
        }
        // 批量转换日期格式
        announcementVOList.stream()
                .peek(announcement -> announcement.setDate(getFormattedDate(announcement.getDate())))
                .collect(Collectors.toList());
        int insertResult = announcementMapper.insertAnnouncementSourceData(announcementVOList, windCode);
        return insertResult > 0;
    }

    /**
     * 转换可插入数据库Date
     *
     * @param date 待转换字符串:2025-06-20T00:00:00Z
     * @return 转换后的代码
     */
    public String getFormattedDate(String date) {
        if (date != null && date.contains(DATE_TIME_SEPARATOR)) {
            return date.replace(DATE_TIME_SEPARATOR, " ").replace(UTC_FLAG, "");
        }
        return date;
    }

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
    @Override
    public List<BigEventVO> getEventSourceData(String windCode, String startDate, String endDate, Integer pageNo, Integer pageSize) {
        String url = String.format(DataSourceConstants.WIND_PROD_WGQ + eventUrl, windCode);
        org.springframework.http.HttpHeaders headers = new HttpHeaders();
        headers.set(DataSourceConstants.WIND_POINT_SESSION_NAME, windSessionId);
        MultiValueMap<String, String> queryParams = new LinkedMultiValueMap<>();
        queryParams.add("startDate", startDate);
        queryParams.add("endDate", endDate);
        queryParams.add("pageNo", String.valueOf(pageNo));
        queryParams.add("pageSize", String.valueOf(pageSize));
        ResponseEntity<String> response = HttpUtil.sendGetWithParams(url, queryParams, headers, 10000, 10000);
        if (!SUCCESS_FLAG.equals(response.getStatusCode().toString())) {
            throw new RuntimeException("getBigEventData_error,result=" + response.getStatusCode());
        }
        List<BigEventVO> eventVOList = JSON.parseObject(response.getBody(), new TypeReference<List<BigEventVO>>() {
        });
        return eventVOList;
    }

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
    @Override
    public Boolean transferEvent(String windCode, String startDate, String endDate, Integer pageNo, Integer pageSize) {
        List<BigEventVO> eventSourceList = getEventSourceData(windCode, startDate, endDate, pageNo, pageSize);
        if (eventSourceList == null || eventSourceList.isEmpty()) {
            log.error("transferEvent_eventSourceList.isEmpty()!windCode={}", windCode);
            return false;
        }
        int insertResult = announcementMapper.insertEventSource(eventSourceList, windCode);
        return insertResult > 0;
    }
}