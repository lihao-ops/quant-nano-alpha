package com.hao.datacollector.dto.f9;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

/**
 * @author LiHao
 * @description: 获取资讯新闻数据传输对象
 */
@Data
@Schema(description = "获取资讯新闻数据传输对象")
public class InformationOceanDTO {

    @Schema(description = "id", required = true)
    private String id;

    @Schema(description = "日期", required = true)
    private String date;

    @Schema(description = "数据来源", required = true)
    private String siteName;

    @Schema(description = "标题", required = true)
    private String title;
}