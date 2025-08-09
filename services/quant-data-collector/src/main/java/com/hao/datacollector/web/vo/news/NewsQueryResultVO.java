package com.hao.datacollector.web.vo.news;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.time.LocalDate;

/**
 * @author hli
 * @program: datacollector
 * @Date 2025-01-15
 * @description: 新闻查询结果VO对象
 */
@Data
@Schema(name = "新闻查询结果传输对象")
public class NewsQueryResultVO {

    @Schema(description = "新闻ID", required = true)
    private String newsId;

    @Schema(description = "标题", required = true)
    private String title;

    @Schema(description = "站点名称", required = true)
    private String sitename;

    @Schema(description = "发布日期", required = true)
    private LocalDate publishDate;

    @Schema(description = "股票代码", required = true)
    private String windCode;
}