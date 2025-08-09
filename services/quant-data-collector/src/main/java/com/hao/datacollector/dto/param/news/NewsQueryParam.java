package com.hao.datacollector.dto.param.news;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.time.LocalDate;

/**
 * @author hli
 * @program: datacollector
 * @Date 2025-01-15
 * @description: 新闻查询参数实体类
 */
@Data
@Schema(name = "新闻查询参数实体类")
public class NewsQueryParam {
    
    @Schema(description = "新闻ID")
    private String newsId;
    
    @Schema(description = "标题")
    private String title;
    
    @Schema(description = "站点名称")
    private String sitename;
    
    @Schema(description = "发布日期开始时间")
    private LocalDate publishDateStart;
    
    @Schema(description = "发布日期结束时间")
    private LocalDate publishDateEnd;
    
    @Schema(description = "股票代码")
    private String windCode;
    
    @Schema(description = "页码（从1开始）")
    private Integer pageNo;
    
    @Schema(description = "每页大小")
    private Integer pageSize;
}