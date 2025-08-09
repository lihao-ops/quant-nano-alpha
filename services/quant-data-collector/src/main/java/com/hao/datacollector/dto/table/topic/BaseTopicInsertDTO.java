package com.hao.datacollector.dto.table.topic;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.io.Serializable;

@Data
@Schema(name = "基础标签表对象")
public class BaseTopicInsertDTO implements Serializable {
    private static final long serialVersionUID = 1L;

    @Schema(description = "标签ID")
    private Integer topicId;

    @Schema(description = "标签名称")
    private String topicName;
}