package com.hao.datacollector.web.controller;

import com.hao.datacollector.dto.param.verification.VerificationQueryParam;
import com.hao.datacollector.service.DataVerificationService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * 数据一致性校验控制器
 *
 * 设计目的：
 * 1. 提供历史数据迁移一致性校验入口。
 * 2. 异步触发校验任务并返回执行提示。
 *
 * 为什么需要该类：
 * - 大批量迁移后需要统一入口进行一致性验证。
 *
 * 核心实现思路：
 * - 将校验请求委派给服务层异步执行，接口只负责触发。
 *
 * @author hli
 * @date 2025-06-05
 * @description 数据一致性校验控制器
 */
@Tag(name = "数据校验", description = "历史数据迁移一致性验证接口")
@Slf4j
@RestController
@RequestMapping("/api/admin/verification")
@RequiredArgsConstructor
public class VerificationController {

    @Autowired
    private DataVerificationService verificationService;

    /**
     * 启动一致性校验
     *
     * 实现逻辑：
     * 1. 记录校验请求参数。
     * 2. 调用服务层异步启动校验。
     * 3. 返回启动提示信息。
     *
     * @param param 校验参数
     * @return 启动提示
     */
    @Operation(summary = "启动一致性校验", description = "异步后台执行，通过日志查看进度")
    @PostMapping("/start")
    public String startVerification(@RequestBody VerificationQueryParam param) {
        // 实现思路：
        // 1. 记录请求并异步触发校验。
        log.info("收到校验请求|Verification_request_received,param={}", param);
        verificationService.startVerification(param);
        return String.format("已启动校验任务! 年份: %s, 目标表: %s. 请查看后台日志跟踪进度。",
                param.getYears(), param.getTargetTableName());
    }
}
