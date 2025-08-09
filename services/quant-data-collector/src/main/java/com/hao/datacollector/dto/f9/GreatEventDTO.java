package com.hao.datacollector.dto.f9;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

/**
 * @author LiHao
 * @description: F9大事数据传输对象
 */
@Data
@Schema(description = "F9大事数据传输对象")
public class GreatEventDTO {
    @Schema(description = "发生日期", required = true)
    private String occureddate;

    @Schema(description = "事件摘要", required = true)
    private String description;
}