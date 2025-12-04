package com.hao.datacollector.common.enums.quotation;

import lombok.Getter;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;

/**
 * 表范围枚举，用于根据日期区间快速定位所属数据表
 *
 * 示例：
 * 2020-01-01 ~ 2023-12-31 → tb_quotation_history_warm
 * 2024-01-01 ~ 2025-12-31 → tb_quotation_history_hot
 * 跨区间则两个表都返回
 *
 * @author hli
 */
@Getter
public enum TableRangeEnum {

    WARM("tb_quotation_history_warm", 2020, 2023),
    HOT("tb_quotation_history_hot", 2024, 2025);

    /** 表名 */
    private final String tableName;
    /** 起始年份 */
    private final int startYear;
    /** 结束年份 */
    private final int endYear;


    TableRangeEnum(String tableName, int startYear, int endYear) {
        this.tableName = tableName;
        this.startYear = startYear;
        this.endYear = endYear;
    }

    /**
     * 根据开始日期和结束日期获取所属表名列表
     */
    public static List<String> resolveTables(String startDate, String endDate, String format) {
        DateTimeFormatter DF = DateTimeFormatter.ofPattern(format);
        LocalDate start = LocalDate.parse(startDate, DF);
        LocalDate end = LocalDate.parse(endDate, DF);

        int startYear = start.getYear();
        int endYear = end.getYear();

        List<String> result = new ArrayList<>();

        for (TableRangeEnum range : TableRangeEnum.values()) {

            // 当前枚举的年份区间与查询区间是否重叠
            if (yearOverlap(startYear, endYear, range.startYear, range.endYear)) {
                result.add(range.tableName);
            }
        }

        return result;
    }

    /**
     * 判断两个年份区间是否有交集
     */
    private static boolean yearOverlap(int s1, int e1, int s2, int e2) {
        return Math.max(s1, s2) <= Math.min(e1, e2);
    }
}
