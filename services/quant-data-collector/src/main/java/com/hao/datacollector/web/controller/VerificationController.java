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
 * =================================================================================
 * 数据一致性校验控制器 (Data Consistency Verification Controller)
 * =================================================================================
 * <p>
 * 核心功能：
 * 触发全量历史行情数据（2020-2021）从"月度分表"迁移到"温数据分区表"后的一致性校验。
 * <p>
 * ---------------------------------------------------------------------------------
 * 🚀 核心实现思路与架构优化策略：
 * ---------------------------------------------------------------------------------
 * <p>
 * 1. 【并发模型设计 - IO密集型优化】
 * - 策略：采用分治思想，将 24 个月份（2020-2021）拆分为 24 个独立的异步任务。
 * - 线程池：完全复用 {@code ThreadPoolConfig} 中的 "ioTaskExecutor"。
 * - 理由：数据库校验是典型的 IO 密集型操作（大量 Select 等待），而非 CPU 计算密集型。
 * 使用针对 IO 优化的线程池（核心线程数 = CPU核数 * 4）可最大化数据库吞吐量。
 * <p>
 * 2. 【源表读取优化 - Keyset Paging / 游标分页】
 * - 痛点：传统 {@code LIMIT 2000 OFFSET 1000000} 在深分页时性能会呈指数级下降（全表扫描）。
 * - 方案：利用联合主键 (wind_code, trade_date) 实现 "Keyset Paging"。
 * - SQL形态：{@code WHERE (wind_code > ? OR (wind_code = ? AND trade_date > ?)) ORDER BY ... LIMIT 2000}
 * - 收益：无论翻到第几页，均利用 B+ 树索引直接定位，时间复杂度恒定为 O(logN)。
 * <p>
 * 3. 【目标表查询优化 - 分区剪枝 & 范围锁定】
 * - 痛点：使用 {@code WHERE IN (...)} 批量查询目标表会导致全表/全分区扫描，且 SQL 解析成本高。
 * - 方案：根据源表当前批次的 "首行" 和 "尾行" 数据，计算出时间范围。
 * - SQL形态：{@code WHERE trade_date >= '本批次开始' AND trade_date <= '本批次结束' ...}
 * - 收益：充分利用目标表 {@code PARTITION BY RANGE COLUMNS(trade_date)} 特性，
 * 实现 "分区剪枝 (Partition Pruning)"，只扫描特定分区内的特定索引范围。
 * <p>
 * 4. 【严谨的比对逻辑】
 * - 数值精度：使用 {@code BigDecimal.compareTo()} 而非 {@code equals()}。
 * 解决数据库中 10.5000 与 Java 中 10.5 被视为不相等的问题。
 * - 内存保护：严格控制 {@code BATCH_SIZE = 2000}，防止多线程并发时 JVM 堆内存溢出 (OOM)。
 * <p>
 * =================================================================================
 */

/**
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
     * 触发全量校验
     * <p>
     * 调用流程：
     * 1. 客户端传入年份列表 (如 2021, 2022) 和目标表名。
     * 2. Service 层按年份拆解为 "月度" 任务 (2年 x 12月 = 24个任务)。
     * 3. 任务提交至 IO 密集型线程池并发执行。
     * 4. 接口立即返回，不阻塞。
     */
    @Operation(summary = "启动一致性校验", description = "异步后台执行，通过日志查看进度")
    @PostMapping("/start")
    public String startVerification(@RequestBody VerificationQueryParam param) {
        log.info("收到校验请求: {}", param);
        // 核心入口
        verificationService.startVerification(param);
        return String.format("已启动校验任务! 年份: %s, 目标表: %s. 请查看后台日志跟踪进度。", param.getYears(), param.getTargetTableName());
    }
}