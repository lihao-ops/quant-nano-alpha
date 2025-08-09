package com.hao.datacollector.web.vo.stock;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.time.LocalDate;

/**
 * @author hli
 * @program datacollector
 * @date 2025-01-15
 * @description 股票基本信息查询结果传输对象
 */
@Data
@Schema(name = "股票基本信息查询结果传输对象")
public class StockBasicInfoQueryResultVO {

    @Schema(description = "股票代码", required = true)
    private String windCode;

    @Schema(description = "股票名称")
    private String windName;

    @Schema(description = "上市日期")
    private LocalDate listingDate;

    @Schema(description = "股票状态")
    private String statusExistence;

    @Schema(description = "概念板块")
    private String conceptPlates;

    @Schema(description = "热门概念")
    private String hotConcepts;

    @Schema(description = "产业链")
    private String industryChain;

    @Schema(description = "是否长期破净")
    private String isLongBelowNetAsset;

    @Schema(description = "公司简介")
    private String companyProfile;

    @Schema(description = "经营范围")
    private String businessScope;

    @Schema(description = "申万行业代码")
    private String swIndustryCode;

    @Schema(description = "申万行业名称")
    private String swIndustryName;

    @Schema(description = "中信行业代码")
    private String citicIndustryCode;

    @Schema(description = "中信行业名称")
    private String citicIndustryName;

    @Schema(description = "总股本")
    private Long totalShares;

    @Schema(description = "流通股本")
    private Long floatShares;

    @Schema(description = "创建时间")
    private java.time.LocalDateTime createTime;

    @Schema(description = "更新时间")
    private java.time.LocalDateTime updateTime;
}