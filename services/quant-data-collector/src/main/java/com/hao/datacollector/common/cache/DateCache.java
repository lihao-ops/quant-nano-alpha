package com.hao.datacollector.common.cache;

import com.hao.datacollector.common.constant.DateTimeFormatConstants;
import com.hao.datacollector.common.utils.DateUtil;
import com.hao.datacollector.service.BaseDataService;
import jakarta.annotation.PostConstruct;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.time.LocalDate;
import java.util.List;

/**
 * @author hli
 * @program: datacollector
 * @Date 2025-06-14 17:34:21
 * @description: 日期相关数据缓存
 */
@Slf4j
@Component("DateCache")
public class DateCache {
    /**
     * 今年整年交易日历
     */
    public static List<LocalDate> ThisYearTradeDateList;

    /**
     * 年初至今的交易日历
     */
    public static List<LocalDate> CurrentYearTradeDateList;
    /**
     * 2020年的交易日历
     */
    public static List<LocalDate> Year2020TradeDateList;

    /**
     * 2021年的交易日历
     */
    public static List<LocalDate> Year2021TradeDateList;
    /**
     * 2022年的交易日历
     */
    public static List<LocalDate> Year2022TradeDateList;

    /**
     * 2023年的交易日历
     */
    public static List<LocalDate> Year2023TradeDateList;

    /**
     * 2024年的交易日历
     */
    public static List<LocalDate> Year2024TradeDateList;


    @Autowired
    private BaseDataService baseDataService;

    @PostConstruct
    private void initDateList() {
        //今年整年的交易日历
        String firstDayOfYear = DateUtil.getFirstDayOfYear(DateTimeFormatConstants.EIGHT_DIGIT_DATE_FORMAT);
        String lastDayOfYear = DateUtil.getLastDayOfYear(DateTimeFormatConstants.EIGHT_DIGIT_DATE_FORMAT);
        ThisYearTradeDateList = baseDataService.getTradeDateListByTime(firstDayOfYear, lastDayOfYear);
        //年初至今的交易日历
        String currentDay = DateUtil.getCurrentDateTime(DateTimeFormatConstants.EIGHT_DIGIT_DATE_FORMAT);
        CurrentYearTradeDateList = baseDataService.getTradeDateListByTime(firstDayOfYear, currentDay);
        //2020年的交易日历
        Year2020TradeDateList = baseDataService.getTradeDateListByTime(DateUtil.getFirstDayOfYear(2020, DateTimeFormatConstants.EIGHT_DIGIT_DATE_FORMAT), DateUtil.getLastDayOfYear(2020, DateTimeFormatConstants.EIGHT_DIGIT_DATE_FORMAT));
        //2021年的交易日历
        Year2021TradeDateList = baseDataService.getTradeDateListByTime(DateUtil.getFirstDayOfYear(2021, DateTimeFormatConstants.EIGHT_DIGIT_DATE_FORMAT), DateUtil.getLastDayOfYear(2021, DateTimeFormatConstants.EIGHT_DIGIT_DATE_FORMAT));
        //2022年的交易日历
        Year2022TradeDateList = baseDataService.getTradeDateListByTime(DateUtil.getFirstDayOfYear(2022, DateTimeFormatConstants.EIGHT_DIGIT_DATE_FORMAT), DateUtil.getLastDayOfYear(2022, DateTimeFormatConstants.EIGHT_DIGIT_DATE_FORMAT));
        //2023年的交易日历
        Year2023TradeDateList = baseDataService.getTradeDateListByTime(DateUtil.getFirstDayOfYear(2023, DateTimeFormatConstants.EIGHT_DIGIT_DATE_FORMAT), DateUtil.getLastDayOfYear(2023, DateTimeFormatConstants.EIGHT_DIGIT_DATE_FORMAT));
        //2024年的交易日历
        Year2024TradeDateList = baseDataService.getTradeDateListByTime(DateUtil.getFirstDayOfYear(2024, DateTimeFormatConstants.EIGHT_DIGIT_DATE_FORMAT), DateUtil.getLastDayOfYear(2024, DateTimeFormatConstants.EIGHT_DIGIT_DATE_FORMAT));
        log.info("CurrentYearTradeDateList.size={},CurrentYearTradeDateList.size={},Year2022TradeDateList.size={},Year2023TradeDateList.size={},Year2024TradeDateList.size={},", ThisYearTradeDateList.size(), CurrentYearTradeDateList.size(), Year2022TradeDateList.size(), Year2023TradeDateList.size(), Year2024TradeDateList.size());
    }
}
