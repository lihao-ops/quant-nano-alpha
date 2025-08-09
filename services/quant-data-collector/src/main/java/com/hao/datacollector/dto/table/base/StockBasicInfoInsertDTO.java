package com.hao.datacollector.dto.table.base;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

@Data
@Schema(description = "上市公司基础信息插入对象")
public class StockBasicInfoInsertDTO {

    @Schema(description = "证券代码", example = "600519.SH")
    private String windCode;

    @Schema(description = "证券简称", example = "贵州茅台")
    private String secName;

    @Schema(description = "上市日期", example = "2001-08-27")
    private String listingDate;

    @Schema(description = "证券存续状态：L=上市；D=摘牌；N=未上市", example = "L")
    private String statusExistence;

    @Schema(description = "所属概念板块")
    private String conceptPlates;

    @Schema(description = "所属热门概念")
    private String hotConcepts;

    @Schema(description = "所属产业链板块")
    private String industryChain;

    @Schema(description = "是否长期破净(1=是, 0=否)", example = "0")
    private Integer isLongBelowNetAsset;

    @Schema(description = "公司简介")
    private String companyProfile;

    @Schema(description = "经营范围")
    private String businessScope;

    @Schema(description = "所属申万行业代码(2014一级行业)", example = "801150")
    private String swIndustryCode;

    @Schema(description = "所属申万行业名称(2014一级行业)", example = "饮料制造")
    private String swIndustryName;

    @Schema(description = "所属中信行业代码(一级行业)", example = "C101")
    private String citicIndustryCode;

    @Schema(description = "所属中信行业名称(一级行业)", example = "食品饮料")
    private String citicIndustryName;

    @Schema(description = "总股本(股)")
    private Double totalShares;

    @Schema(description = "流通A股(股)")
    private Double floatShares;
}
