package com.hao.datacollector.web.vo.announcement;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

/**
 * @author hli
 * @description 大事VO对象
 */
@Data
@Schema(description = "大事VO对象")
public class BigEventVO {

    @Schema(description = "发生日期", required = true)
    private String date;

    @Schema(description = "事件标题", required = true)
    private String event;

    @Schema(description = "事件类型", required = true)
    private String type;
}