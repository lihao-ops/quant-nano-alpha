package com.hao.datacollector.dto.param.stock;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.time.LocalDate;

/**
 * @author hli
 * @program datacollector
 * @date 2025-01-15
 * @description 股票基本信息查询参数实体类
 */
@Data
@Schema(name = "股票基本信息查询参数实体类")
public class StockBasicInfoQueryParam {

    @Schema(description = "股票代码")
    private String windCode;

    @Schema(description = "股票名称")
    private String windName;

    @Schema(description = "申万行业代码")
    private String swIndustryCode;

    @Schema(description = "申万行业名称")
    private String swIndustryName;

    @Schema(description = "中信行业代码")
    private String citicIndustryCode;

    @Schema(description = "中信行业名称")
    private String citicIndustryName;

    @Schema(description = "上市日期开始")
    private LocalDate listingDateStart;

    @Schema(description = "上市日期结束")
    private LocalDate listingDateEnd;

    @Schema(description = "股票状态")
    private String statusExistence;

    @Schema(description = "概念板块")
    private String conceptPlates;

    @Schema(description = "热门概念")
    private String hotConcepts;

    @Schema(description = "产业链")
    private String industryChain;

    @Schema(description = "是否长期跌破净资产")
    private String isLongBelowNetAsset;

    @Schema(description = "公司简介")
    private String companyProfile;

    @Schema(description = "经营范围")
    private String businessScope;

    @Schema(description = "总股本最小值")
    private Long totalSharesMin;

    @Schema(description = "总股本最大值")
    private Long totalSharesMax;

    @Schema(description = "流通股本最小值")
    private Long floatSharesMin;

    @Schema(description = "流通股本最大值")
    private Long floatSharesMax;

    @Schema(description = "页码", example = "1")
    private Integer pageNo = 1;

    @Schema(description = "每页大小", example = "10")
    private Integer pageSize = 10;
}