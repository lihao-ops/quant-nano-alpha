package com.hao.datacollector.web.controller;


import com.hao.datacollector.service.AnnouncementService;
import com.hao.datacollector.web.vo.announcement.AnnouncementVO;
import com.hao.datacollector.web.vo.announcement.BigEventVO;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.Parameters;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseBody;

import java.util.List;

/**
 * @author hli
 * @date 2025-06-24 13:45
 * @description 公告数据接口controller
 */
@Tag(name = "个股公告页数据")
@Controller
@RequestMapping("stock")
public class AnnouncementController {

    @Autowired
    private AnnouncementService announcementService;

    @Operation(summary = "个股公告数据源", method = "GET")
    @Parameters({
            @Parameter(name = "windCode", description = "股票代码", required = true, schema = @Schema(type = "string")),
            @Parameter(name = "startDate", description = "起始日期", required = true, schema = @Schema(type = "string")),
            @Parameter(name = "endDate", description = "结束日期", required = true, schema = @Schema(type = "string")),
            @Parameter(name = "pageNo", description = "页号", required = true, schema = @Schema(type = "integer")),
            @Parameter(name = "pageSize", description = "页面规模", required = true, schema = @Schema(type = "integer"))
    })
    @RequestMapping("/announcement/{windCode}")
    @ResponseBody
    public List<AnnouncementVO> getAnnouncementSourceData(@PathVariable String windCode, @RequestParam String startDate, @RequestParam String endDate, @RequestParam Integer pageNo, @RequestParam Integer pageSize) {
        return announcementService.getAnnouncementSourceData(windCode, startDate, endDate, pageNo, pageSize);
    }

    /**
     * 个股大事数据源
     *
     * @param windCode  股票代码
     * @param startDate 起始日期
     * @param endDate   结束日期
     * @param pageNo    页号
     * @param pageSize  页面规模
     * @return 个股大事数据源
     */
    @Operation(summary = "个股大事数据", method = "GET")
    @Parameters({
            @Parameter(name = "windCode", description = "股票代码", required = true, schema = @Schema(type = "string")),
            @Parameter(name = "startDate", description = "起始日期，格式yyyyMMdd", required = true, schema = @Schema(type = "string")),
            @Parameter(name = "endDate", description = "结束日期，格式yyyyMMdd", required = true, schema = @Schema(type = "string")),
            @Parameter(name = "pageNo", description = "页号", required = true, schema = @Schema(type = "integer")),
            @Parameter(name = "pageSize", description = "页面规模", required = true, schema = @Schema(type = "integer"))
    })
    @RequestMapping("/event/{windCode}")
    @ResponseBody
    public List<BigEventVO> getEventSourceData(@PathVariable String windCode, @RequestParam String startDate, @RequestParam String endDate, @RequestParam Integer pageNo, @RequestParam Integer pageSize) {
        return announcementService.getEventSourceData(windCode, startDate, endDate, pageNo, pageSize);
    }
}