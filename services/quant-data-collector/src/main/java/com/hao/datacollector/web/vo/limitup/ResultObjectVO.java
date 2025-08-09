package com.hao.datacollector.web.vo.limitup;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.util.List;
import java.util.Map;

@Data
@Schema(name = "ResultObjectVO")
public class ResultObjectVO extends BaseVO {
    private static final long serialVersionUID = 1L;

    @Schema(description = "股票数量对象")
    private Map<String, String> stockNums;

    @Schema(description = "股票列表对象")
    private List<TopicStockVO> stockDetail;

    @Schema(description = "标签数据")
    private Map<String, TopicInfoVO> topicList;
}