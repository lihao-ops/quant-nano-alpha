package com.hao.datacollector.web.controller;

import com.hao.datacollector.dto.param.news.NewsQueryParam;
import com.hao.datacollector.service.NewsService;
import com.hao.datacollector.web.vo.news.NewsQueryResultVO;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.util.List;

/**
 * @author hli
 * @program: datacollector
 * @Date 2025-06-20 17:09:27
 * @description: 新闻相关controller
 */
@Tag(name = "新闻模块", description = "新闻模块数据处理接口")
@Slf4j
@RestController
@RequestMapping("/news")
public class NewsController {
    @Autowired
    private NewsService newsService;

    @Operation(summary = "转档股票新闻数据", description = "检查服务运行状态")
    @ApiResponses(value = {
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "服务正常运行"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "500", description = "服务异常")
    })
    @PostMapping("/transfer")
    public ResponseEntity<String> transferNewsStockData(@RequestParam(required = false) String windCode) {
        Boolean success = newsService.transferNewsStockData(windCode);
        if (!success) {
            return ResponseEntity.badRequest().body("数据转档失败");
        }
        return ResponseEntity.ok("数据转档成功");
    }

    @Operation(summary = "查询新闻基础数据", description = "根据条件查询新闻基础数据")
    @ApiResponses(value = {
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "查询成功"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "500", description = "查询异常")
    })
    @GetMapping("/query")
    public ResponseEntity<List<NewsQueryResultVO>> queryNewsBaseData(
            @RequestParam(required = false) String newsId,
            @RequestParam(required = false) String title,
            @RequestParam(required = false) String sitename,
            @RequestParam(required = false) @DateTimeFormat(pattern = "yyyy-MM-dd") LocalDate publishDateStart,
            @RequestParam(required = false) @DateTimeFormat(pattern = "yyyy-MM-dd") LocalDate publishDateEnd,
            @RequestParam(required = false) String windCode,
            @RequestParam(required = false, defaultValue = "1") Integer pageNo,
            @RequestParam(required = false, defaultValue = "10") Integer pageSize) {
        NewsQueryParam queryParam = new NewsQueryParam();
        queryParam.setNewsId(newsId);
        queryParam.setTitle(title);
        queryParam.setSitename(sitename);
        queryParam.setPublishDateStart(publishDateStart);
        queryParam.setPublishDateEnd(publishDateEnd);
        queryParam.setWindCode(windCode);
        queryParam.setPageNo(pageNo);
        queryParam.setPageSize(pageSize);
        
        List<NewsQueryResultVO> result = newsService.queryNewsBaseData(queryParam);
        return ResponseEntity.ok(result);
    }
}
