package com.hao.datacollector.web.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;

@Tag(name = "系统管理", description = "系统健康检查和状态监控接口")
@RestController
@RequestMapping("health")
public class HealthController {

    @Value("${spring.application.name:datacollector}")
    private String applicationName;

    @Operation(summary = "健康检查", description = "检查服务运行状态")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "服务正常运行"),
            @ApiResponse(responseCode = "500", description = "服务异常")
    })
    @GetMapping("/check")
    public ResponseEntity<Map<String, Object>> healthCheck() {
        Map<String, Object> response = new HashMap<>();
        response.put("status", "UP");
        response.put("service", applicationName);
        response.put("timestamp", LocalDateTime.now());
        response.put("description", "量化交易数据收集器服务运行正常");
        return ResponseEntity.ok(response);
    }

    @Operation(summary = "服务信息", description = "获取服务基本信息")
    @GetMapping("/info")
    public ResponseEntity<Map<String, Object>> serviceInfo() {
        Map<String, Object> info = new HashMap<>();
        info.put("name", applicationName);
        info.put("version", "1.0.0");
        info.put("description", "量化交易数据服务模块：实现数据采集、清洗、存储等任务");
        info.put("uptime", LocalDateTime.now());
        return ResponseEntity.ok(info);
    }
}