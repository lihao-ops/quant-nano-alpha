package com.hao.datacollector.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

/**
 * @author LiHao
 * @description: 分页数据传输对象
 * @Date 2023-09-13 11:03:41
 */
@Data
@Schema(description = "分页数据传输对象")
public class PageNumDTO {
    @Schema(description = "页码", required = true)
    private Integer pageNo;

    @Schema(description = "每页数量", required = true)
    private Integer pageSize;
}