package com.hao.datacollector.web.vo.limitup;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

@Data
@Schema(name = "TopicInfoVO")
public class TopicInfoVO extends BaseVO {
    private static final long serialVersionUID = 1L;
    // @Schema(description = "windCode")
// private String windCode;
    @Schema(description = "标签ID")
    private int topicId;

    @Schema(description = "标签颜色")
    private String color;

    @Schema(description = "标签股票数量")
    private int stockNum;

    @Schema(description = "标签名称")
    private String topic;

    @Schema(description = "标签热度")
    private double topicHot;
}