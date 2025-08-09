package com.hao.datacollector.web.vo.topic;

import com.hao.datacollector.dto.table.topic.InsertTopicInfoDTO;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

/**
 * @author Hao Li
 * @Date 2025-07-24 15:01:37
 * @description: 热门题材信息VO对象(kpl)
 */
@Data
@Schema(description = "热门题材信息VO对象(kpl)")
public class TopicInfoKplVO extends InsertTopicInfoDTO {
    @Schema(description = "数据总量", example = "100")
    private Integer totalNum;
}