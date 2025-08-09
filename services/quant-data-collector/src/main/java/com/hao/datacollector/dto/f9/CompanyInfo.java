package com.hao.datacollector.dto.f9;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

/**
 * @description: 公司信息
 * @date 2023-08-01 14:49:05
 */
@Data
@Schema(description = "获取公司信息")
public class CompanyInfo {
    @Schema(description = "公司名称", required = true)
    private String cpyName;

    @Schema(description = "公司行业", required = true)
    private String windIndustry;

    @Schema(description = "成立日期", required = true)
    private String inceptonDate;

    @Schema(description = "上市日期", required = true)
    private String ipoListedDate;

    @Schema(description = "注册资本", required = true)
    private String registeredcapital;

    @Schema(description = "注册资本单位", required = true)
    private String currencys;

    @Schema(description = "注册地址", required = true)
    private String registeredAddress;

    @Schema(description = "办公地址", required = true)
    private String officeAddress;

    @Schema(description = "员工总数", required = true)
    private String employeeNumbers;

    @Schema(description = "董事长", required = true)
    private String chairman;

    @Schema(description = "总经理", required = true)
    private String generalmanager;

    @Schema(description = "实际控制人", required = true)
    private String holderController;

    @Schema(description = "第一股东", required = true)
    private String holderName;

    @Schema(description = "第一股东占有比例", required = true)
    private Double holderPct;

    @Schema(description = "公司网站", required = true)
    private String webSite;
}