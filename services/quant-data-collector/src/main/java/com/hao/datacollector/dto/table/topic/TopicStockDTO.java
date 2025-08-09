package com.hao.datacollector.dto.table.topic;

import com.hao.datacollector.dto.table.base.StockBaseDTO;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

@Schema(description = "题材Id映射股票对象")
@Data
public class TopicStockDTO extends StockBaseDTO {
    @Schema(description = "题材id", example = "25")
    private Integer topicId;
}
