package com.hao.datacollector.dto.table.limitup;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.io.Serializable;
import java.sql.Timestamp;
import java.time.LocalDate;

@Data
@Schema(name = "涨停标签关联表")
public class LimitUpStockTopicRelationInsertDTO implements Serializable {
    private static final long serialVersionUID = 1L;

    @Schema(description = "股票代码")
    private String windCode;

    @Schema(description = "交易日期")
    private LocalDate tradeDate;

    @Schema(description = "标签ID")
    private Integer topicId;

    @Schema(description = "标签颜色")
    private String color;

    @Schema(description = "标签股票数量")
    private Integer stockNum;

    @Schema(description = "标签热度")
    private Double topicHot;
}