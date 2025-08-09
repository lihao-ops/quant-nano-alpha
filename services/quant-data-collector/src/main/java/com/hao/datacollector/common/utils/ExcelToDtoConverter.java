package com.hao.datacollector.common.utils;

import com.hao.datacollector.dto.table.base.StockBasicInfoInsertDTO;
import com.hao.datacollector.dto.table.base.StockFinancialMetricsInsertDTO;
import org.apache.commons.lang3.StringUtils;

import java.io.File;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import static com.hao.datacollector.common.utils.ExcelReaderUtil.readHeaders;

public class ExcelToDtoConverter {

    /**
     * 表字段名list
     */
    public static List<String> headers = new ArrayList<>();

    public static List<StockBasicInfoInsertDTO> convertToBasicInfoDTO(List<Map<String, String>> rows) {
        List<StockBasicInfoInsertDTO> list = new ArrayList<>();
        for (Map<String, String> row : rows) {
            StockBasicInfoInsertDTO dto = new StockBasicInfoInsertDTO();
            //getValueByFuzzyKey(row, headers,
            dto.setWindCode(getValueByFuzzyKey(row, headers, "证券代码"));
            dto.setSecName(getValueByFuzzyKey(row, headers, "证券简称"));
            dto.setListingDate(getValueByFuzzyKey(row, headers, "上市日期"));
            dto.setStatusExistence(getValueByFuzzyKey(row, headers, "证券存续状态"));
            dto.setConceptPlates(getValueByFuzzyKey(row, headers, "所属概念板块"));
            dto.setHotConcepts(getValueByFuzzyKey(row, headers, "所属热门概念"));
            dto.setIndustryChain(getValueByFuzzyKey(row, headers, "所属产业链板块"));
            dto.setIsLongBelowNetAsset(parseBoolean(getValueByFuzzyKey(row, headers, "是否长期破净")));
            dto.setCompanyProfile(getValueByFuzzyKey(row, headers, "公司简介"));
            dto.setBusinessScope(getValueByFuzzyKey(row, headers, "经营范围"));
            dto.setSwIndustryCode(getValueByFuzzyKey(row, headers, "所属申万行业代码"));
            dto.setSwIndustryName(getValueByFuzzyKey(row, headers, "所属申万行业名称"));
            dto.setCiticIndustryCode(getValueByFuzzyKey(row, headers, "所属中信行业代码"));
            dto.setCiticIndustryName(getValueByFuzzyKey(row, headers, "所属中信行业名称"));
            String totalShares = getValueByFuzzyKey(row, headers, "总股本");
            dto.setTotalShares(StringUtils.isEmpty(totalShares) ? null : Double.parseDouble(totalShares));
            String floatShares = getValueByFuzzyKey(row, headers, "流通A股");
            dto.setFloatShares(StringUtils.isEmpty(floatShares) ? null : Double.parseDouble(floatShares));
            list.add(dto);
        }
        return list;
    }

    public static List<StockFinancialMetricsInsertDTO> convertToFinancialMetricsDTO(List<Map<String, String>> rows) {
        List<StockFinancialMetricsInsertDTO> list = new ArrayList<>();
        for (Map<String, String> row : rows) {
            StockFinancialMetricsInsertDTO dto = new StockFinancialMetricsInsertDTO();
            dto.setWindCode(getValueByFuzzyKey(row, headers, "证券代码"));
//            dto.setTradeDate(getValueByFuzzyKey(row, headers, "交易日期"));
            dto.setTradeDate("2025-05-30");
            dto.setShareholderHoldingsChange(parseDecimal(getValueByFuzzyKey(row, headers, "重要股东二级市场交易区间持仓市值变动")));
            dto.setTotalMarketCap(parseDecimal(getValueByFuzzyKey(row, headers, "A股市值")));
            dto.setTop10FloatHoldersRatio(parseDecimal(getValueByFuzzyKey(row, headers, "前十大流通股东持股比例合计")));
            String instBuyTimes = getValueByFuzzyKey(row, headers, "机构席位买入次数");
            dto.setInstBuyTimes(StringUtils.isEmpty(instBuyTimes) ? null : Double.valueOf(instBuyTimes));
            dto.setInstHolderNames(getValueByFuzzyKey(row, headers, "机构股东名称"));
            String instHoldingsTotal = getValueByFuzzyKey(row, headers, "机构持股数量合计");
            dto.setInstHoldingsTotal(StringUtils.isEmpty(instHoldingsTotal) ? null : Double.valueOf(instHoldingsTotal));
            dto.setInstHolderTypes(getValueByFuzzyKey(row, headers, "机构股东类型"));
            dto.setPeTtm(parseDecimal(getValueByFuzzyKey(row, headers, "市盈率PE(TTM)")));
            dto.setRatingScore(parseDecimal(getValueByFuzzyKey(row, headers, "综合评级(数值)")));
            dto.setRatingText(getValueByFuzzyKey(row, headers, "综合评级(中文)"));
            String ratingAgencyCount = getValueByFuzzyKey(row, headers, "评级机构家数");
            dto.setRatingAgencyCount(StringUtils.isEmpty(ratingAgencyCount) ? null : Double.valueOf(ratingAgencyCount));
            dto.setTargetPrice(parseDecimal(getValueByFuzzyKey(row, headers, "一致预测目标价")));
            dto.setPb(parseDecimal(getValueByFuzzyKey(row, headers, "市净率PB")));
            dto.setPe(parseDecimal(getValueByFuzzyKey(row, headers, "市盈率PE")));
            dto.setPs(parseDecimal(getValueByFuzzyKey(row, headers, "市销率PS")));
            dto.setNetProfitTtm(parseDecimal(getValueByFuzzyKey(row, headers, "净利润(TTM)")));
            dto.setRoeGrowth3y(parseDecimal(getValueByFuzzyKey(row, headers, "净资产收益率(N年,增长率)")));
            dto.setDividendYield(parseDecimal(getValueByFuzzyKey(row, headers, "股息率(近12个月)")));
            dto.setEsgScore(parseDecimal(getValueByFuzzyKey(row, headers, "Wind ESG综合得分")));
            String patentCount = getValueByFuzzyKey(row, headers, "发明专利个数");
            dto.setPatentCount(StringUtils.isEmpty(patentCount) ? null : Double.valueOf(patentCount));
            dto.setEsgControversyScore(parseDecimal(getValueByFuzzyKey(row, headers, "ESG争议事件得分")));
            dto.setRoa(parseDecimal(getValueByFuzzyKey(row, headers, "总资产净利率ROA")));
            dto.setNetProfitMargin(parseDecimal(getValueByFuzzyKey(row, headers, "净利润/营业总收入(TTM)")));
            dto.setMaTrend(getValueByFuzzyKey(row, headers, "均线多空头排列看涨看跌"));
            dto.setRsi6(parseDecimal(getValueByFuzzyKey(row, headers, "RSI相对强弱指标")));
            list.add(dto);
        }
        return list;
    }

    /**
     * 模糊取列
     *
     * @param row
     * @param headers
     * @param keyPart
     * @return
     */
    private static String getValueByFuzzyKey(Map<String, String> row, List<String> headers, String keyPart) {
        for (String header : headers) {
            if (header.contains(keyPart)) {
                return row.get(header);
            }
        }
        return null;
    }

    // 工具方法
    private static LocalDate parseDate(String str) {
        try {
            return LocalDate.parse(str, DateTimeFormatter.ofPattern("yyyy-MM-dd"));
        } catch (Exception e) {
            return null;
        }
    }

    private static BigDecimal parseDecimal(String str) {
        try {
            return new BigDecimal(str.replace(",", ""));
        } catch (Exception e) {
            return null;
        }
    }

    private static Integer parseInt(String str) {
        try {
            return Integer.parseInt(str.replace(",", ""));
        } catch (Exception e) {
            return null;
        }
    }

    private static Long parseLong(String str) {
        try {
            return Long.parseLong(str.replace(",", ""));
        } catch (Exception e) {
            return null;
        }
    }

    private static int parseBoolean(String str) {
        if ("1".equals(str) || "是".equalsIgnoreCase(str)) {
            return 1;
        }
        return 0;
    }

    public static void main(String[] args) {
        File file = new File("C:\\Users\\lihao\\Desktop\\data\\基本信息.xlsx");
        headers = readHeaders(file);
        System.out.println("读取到表头：");
        headers.forEach(System.out::println);
        List<Map<String, String>> dataList = ExcelReaderUtil.readExcel(file);
        // 转 DTO
        List<StockBasicInfoInsertDTO> basicInfoList = ExcelToDtoConverter.convertToBasicInfoDTO(dataList);
        List<StockFinancialMetricsInsertDTO> metricsList = ExcelToDtoConverter.convertToFinancialMetricsDTO(dataList);
        // 你可以根据需要进行数据库插入、校验或日志输出
        System.out.println("basicInfoList=" + basicInfoList.get(0));
        System.out.println("metricsList=" + metricsList.get(0));
    }
}
