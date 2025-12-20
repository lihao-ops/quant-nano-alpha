package com.hao.datacollector.common.utils;

import com.hao.datacollector.dto.table.base.StockBasicInfoInsertDTO;
import com.hao.datacollector.dto.table.base.StockFinancialMetricsInsertDTO;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.math.BigDecimal;
import java.nio.file.Paths;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import static com.hao.datacollector.common.utils.ExcelReaderUtil.readHeaders;

/**
 * Excel数据转DTO工具类
 *
 * 职责：将Excel读取结果转换为业务DTO，屏蔽字段映射细节。
 *
 * 设计目的：
 * 1. 统一Excel列名与DTO字段的映射逻辑。
 * 2. 减少业务层重复的字段解析与类型转换。
 *
 * 为什么需要该类：
 * - Excel结构变化频繁，需要集中维护解析规则。
 *
 * 核心实现思路：
 * - 先读取表头并按模糊匹配取值，再填充DTO并返回。
 */
public class ExcelToDtoConverter {
    private static final Logger LOG = LoggerFactory.getLogger(ExcelToDtoConverter.class);

    /**
     * 表头字段列表
     */
    public static List<String> headers = new ArrayList<>();

    /**
     * 转换基础信息DTO列表
     *
     * 实现逻辑：
     * 1. 遍历行数据并按表头模糊匹配字段。
     * 2. 填充StockBasicInfoInsertDTO并收集返回。
     *
     * @param rows Excel行数据
     * @return 基础信息DTO列表
     */
    public static List<StockBasicInfoInsertDTO> convertToBasicInfoDTO(List<Map<String, String>> rows) {
        // 实现思路：按表头模糊匹配取值并填充DTO
        List<StockBasicInfoInsertDTO> list = new ArrayList<>();
        for (Map<String, String> row : rows) {
            StockBasicInfoInsertDTO dto = new StockBasicInfoInsertDTO();
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

    /**
     * 转换财务指标DTO列表
     *
     * 实现逻辑：
     * 1. 遍历行数据并按表头模糊匹配字段。
     * 2. 填充StockFinancialMetricsInsertDTO并收集返回。
     *
     * @param rows Excel行数据
     * @return 财务指标DTO列表
     */
    public static List<StockFinancialMetricsInsertDTO> convertToFinancialMetricsDTO(List<Map<String, String>> rows) {
        // 实现思路：按表头匹配并转换为数值类型字段
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
     * 实现逻辑：
     * 1. 遍历表头列表。
     * 2. 找到包含关键字的列并返回对应值。
     *
     * @param row     行数据
     * @param headers 表头列表
     * @param keyPart 关键字片段
     * @return 匹配到的单元格内容
     */
    private static String getValueByFuzzyKey(Map<String, String> row, List<String> headers, String keyPart) {
        // 实现思路：通过包含关系匹配列名
        for (String header : headers) {
            if (header.contains(keyPart)) {
                return row.get(header);
            }
        }
        return null;
    }

    /**
     * 解析日期
     *
     * 实现逻辑：
     * 1. 按固定格式解析字符串。
     * 2. 异常时返回null。
     *
     * @param str 日期字符串
     * @return 本地日期
     */
    private static LocalDate parseDate(String str) {
        // 实现思路：异常兜底返回空值
        try {
            return LocalDate.parse(str, DateTimeFormatter.ofPattern("yyyy-MM-dd"));
        } catch (Exception e) {
            return null;
        }
    }

    /**
     * 解析小数
     *
     * 实现逻辑：
     * 1. 去除分隔符并转换为BigDecimal。
     * 2. 异常时返回null。
     *
     * @param str 数值字符串
     * @return BigDecimal结果
     */
    private static BigDecimal parseDecimal(String str) {
        // 实现思路：统一去逗号并转换
        try {
            return new BigDecimal(str.replace(",", ""));
        } catch (Exception e) {
            return null;
        }
    }

    /**
     * 解析整数
     *
     * 实现逻辑：
     * 1. 去除分隔符并转换为整数。
     * 2. 异常时返回null。
     *
     * @param str 数值字符串
     * @return 整数结果
     */
    private static Integer parseInt(String str) {
        // 实现思路：统一去逗号并转换
        try {
            return Integer.parseInt(str.replace(",", ""));
        } catch (Exception e) {
            return null;
        }
    }

    /**
     * 解析长整数
     *
     * 实现逻辑：
     * 1. 去除分隔符并转换为长整数。
     * 2. 异常时返回null。
     *
     * @param str 数值字符串
     * @return 长整数结果
     */
    private static Long parseLong(String str) {
        // 实现思路：统一去逗号并转换
        try {
            return Long.parseLong(str.replace(",", ""));
        } catch (Exception e) {
            return null;
        }
    }

    /**
     * 解析布尔型字段
     *
     * 实现逻辑：
     * 1. 将"1"或"是"判定为真。
     * 2. 其他情况返回0。
     *
     * @param str 字符串值
     * @return 1表示真，0表示假
     */
    private static int parseBoolean(String str) {
        // 实现思路：按约定字符串判断布尔值
        if ("1".equals(str) || "是".equalsIgnoreCase(str)) {
            return 1;
        }
        return 0;
    }

    /**
     * Excel转DTO演示入口
     *
     * 实现逻辑：
     * 1. 读取指定Excel文件表头与数据。
     * 2. 转换为DTO并输出示例数据。
     *
     * @param args 启动参数
     */
    public static void main(String[] args) {
        // 实现思路：使用相对路径避免平台绑定
        File file = Paths.get("data", "基本信息.xlsx").toFile();
        headers = readHeaders(file);
        LOG.info("读取表头完成|Read_headers_done,headerSize={}", headers.size());
        headers.forEach(header -> LOG.info("表头内容|Header_item,header={}", header));
        List<Map<String, String>> dataList = ExcelReaderUtil.readExcel(file);
        // 转DTO
        List<StockBasicInfoInsertDTO> basicInfoList = ExcelToDtoConverter.convertToBasicInfoDTO(dataList);
        List<StockFinancialMetricsInsertDTO> metricsList = ExcelToDtoConverter.convertToFinancialMetricsDTO(dataList);
        // 你可以根据需要进行数据库插入、校验或日志输出
        LOG.info("基础信息示例|Basic_info_sample,data={}", basicInfoList.isEmpty() ? null : basicInfoList.get(0));
        LOG.info("财务指标示例|Financial_metrics_sample,data={}", metricsList.isEmpty() ? null : metricsList.get(0));
    }
}
