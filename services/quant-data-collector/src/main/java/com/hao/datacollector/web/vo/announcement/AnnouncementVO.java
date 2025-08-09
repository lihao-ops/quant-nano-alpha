package com.hao.datacollector.web.vo.announcement;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

/**
 * @author hli
 * @description 公告VO对象
 */
@Data
@Schema(description = "公告VO对象")
public class AnnouncementVO {

    @Schema(description = "日期", required = true)
    private String date;

    @Schema(description = "标题", required = true)
    private String announcement;

    @Schema(description = "访问链接", required = true)
    private String url;
}