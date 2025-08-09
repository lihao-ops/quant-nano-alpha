package com.hao.datacollector.web.controller;

import com.hao.datacollector.service.StockProfileService;
import com.hao.datacollector.web.vo.stockProfile.SearchKeyBoardVO;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.Parameters;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * @author hli
 * @program: datacollector
 * @Date 2025-07-22 19:08:20
 * @description: 个股资料相关接口
 */
@Tag(name = "个股资料相关接口")
@RestController
@RequestMapping("stock_profile")
public class StockProfileController {
    @Autowired
    private StockProfileService stockProfileService;

    @Operation(summary = "键盘精灵数据", method = "GET")
    @Parameters({
            @Parameter(name = "keyword", description = "搜索关键词", required = true, schema = @Schema(type = "String")),
            @Parameter(name = "pageNo", description = "页号（从1开始）", required = true, schema = @Schema(type = "Integer")),
            @Parameter(name = "pageSize", description = "每页大小（从1开始）", required = true, schema = @Schema(type = "Integer"))
    })
    @GetMapping("/search_key_board")
    @ResponseBody
    public List<SearchKeyBoardVO> getSearchKeyBoard(@RequestParam String keyword,
                                                    @RequestParam(required = false, defaultValue = "1") Integer pageNo,
                                                    @RequestParam(required = false, defaultValue = "10") Integer pageSize) {
        return stockProfileService.getSearchKeyBoard(keyword, pageNo, pageSize);
    }
}
