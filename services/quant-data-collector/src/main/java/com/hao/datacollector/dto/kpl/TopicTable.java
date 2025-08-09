package com.hao.datacollector.dto.kpl;

import com.fasterxml.jackson.annotation.JsonProperty;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.util.List;

@Data
@Schema(description = "主题分类表DTO")
public class TopicTable {
    @JsonProperty("Level1")
    @Schema(description = "一级分类")
    private CategoryLevel level1;

    @JsonProperty("Level2")
    @Schema(description = "二级分类列表")
    private List<CategoryLevel> level2;
}