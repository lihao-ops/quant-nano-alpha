package com.hao.datacollector.dto.f9;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

/**
 * @author LiHao
 * @description: 获取公司简介信息数据传输对象
 */
@Data
@Schema(description = "获取公司简介信息数据传输对象")
public class CompanyProfileDTO {
    @Schema(description = "公司简介", required = true)
    private String cpyIntro;

    @Schema(description = "ESG评级", required = true)
    private String rating;

    @Schema(description = "ESG综合得分:总分固定10.00", required = true)
    private Double score;

    @Schema(description = "行业排名", required = true)
    private Integer ranking3;

    @Schema(description = "行业总数", required = true)
    private Integer industryLevel3Count;
}