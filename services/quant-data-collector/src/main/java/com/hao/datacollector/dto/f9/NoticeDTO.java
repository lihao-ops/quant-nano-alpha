package com.hao.datacollector.dto.f9;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

/**
 * @author LiHao
 * @description: F9公告数据传输对象
 */
@Data
@Schema(description = "F9公告数据传输对象")
public class NoticeDTO {
    @Schema(description = "日期", required = true)
    private String date;

    @Schema(description = "标题", required = true)
    private String title;
}