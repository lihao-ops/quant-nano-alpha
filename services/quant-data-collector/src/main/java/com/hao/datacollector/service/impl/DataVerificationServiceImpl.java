package com.hao.datacollector.service.impl;

import com.hao.datacollector.dal.dao.DataVerificationMapper;
import com.hao.datacollector.dto.param.verification.VerificationQueryParam;
import com.hao.datacollector.dto.table.verification.QuotationVerificationDTO;
import com.hao.datacollector.service.DataVerificationService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Lazy;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.springframework.util.StopWatch;

import java.math.BigDecimal;
import java.time.YearMonth;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.stream.Collectors;

/**
 * @author hli
 * @date 2025-06-05
 * @description 数据一致性校验服务实现类 (IO密集型优化版)
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class DataVerificationServiceImpl implements DataVerificationService {

    // 1. 正常的构造器注入 (Lombok处理)
    private final DataVerificationMapper dataVerificationMapper;

    //  核心修改 A：注入自己
    // 必须用 org.springframework.context.annotation.Lazy
    @Autowired
    @Lazy
    private DataVerificationService self;

    // 批次大小，控制内存占用
    private static final int BATCH_SIZE = 2000;

    @Override
    public void startVerification(VerificationQueryParam param) {
        log.info("==========_开始全量数据一致性校验,_目标表:_{}_==========|Log_message", param.getTargetTableName());
        List<CompletableFuture<String>> futures = new ArrayList<>();
        // 遍历输入的年份列表
        for (String yearStr : param.getYears()) {
            try {
                int year = Integer.parseInt(yearStr);
                // 遍历该年的 12 个月
                for (int month = 1; month <= 12; month++) {
                    String yearMonth = String.format("%d%02d", year, month);
                    //  核心修改 B：必须用 self. 调用！！！
                    // 原代码: futures.add(verifyMonthTableAsync(...)); -> 这是 this. 调用，串行！
                    // 新代码:
                    futures.add(self.verifyMonthTableAsync(yearMonth, param.getSourceTableName(), param.getTargetTableName()));
                }
            } catch (NumberFormatException e) {
                log.error("日志记录|Log_message,year_format_error,yearStr={}", yearStr, e);
            }
        }
        // 异步等待所有结果 (可选)
        CompletableFuture.allOf(futures.toArray(new CompletableFuture[0])).thenAccept(v -> log.info("==========_所有校验任务提交完成_==========|Log_message"));
    }

    /**
     * 异步校验单个月份表
     * 关键点：使用 @Async("ioTaskExecutor") 复用配置好的 IO 线程池
     *  注意：此方法必须在接口中定义，否则 self.verifyMonthTableAsync 会编译报错
     */
    @Override // 建议加上 @Override 强约束
    @Async("ioTaskExecutor")
    public CompletableFuture<String> verifyMonthTableAsync(String yearMonth, String sourceTableName, String targetTable) {
        // 旧代码（物理分表）
        // String sourceTable = "tb_quotation_history_trend_" + yearMonth;
        //  新代码（分区表指定分区查询）
        // 假设你的分区命名规则是 pYYYYMM (例如 p202101)
        String partitionName = "p" + yearMonth;
        // 构造出来的字符串类似： a_share_quant.tb_quotation_history_warm PARTITION (p202101)
        String sourceTable = String.format("%s PARTITION (%s)", sourceTableName, partitionName);
        StopWatch stopWatch = new StopWatch(sourceTable);
        stopWatch.start();
        log.info("[{}]_校验启动...|Log_message", sourceTable);
        try {
            // 1. 计算时间范围，用于目标表的分区剪枝
            YearMonth ym = YearMonth.parse(yearMonth, DateTimeFormatter.ofPattern("yyyyMM"));
            String startDate = ym.atDay(1).toString() + " 00:00:00";
            String endDate = ym.plusMonths(1).atDay(1).toString() + " 00:00:00";
            // 2. 阶段一：总量快速比对
            // Mybatis 动态 SQL 执行
            Long sourceCount = dataVerificationMapper.countTable(sourceTable);
            Long targetCount = dataVerificationMapper.countTargetByRange(targetTable, startDate, endDate);
            if (!sourceCount.equals(targetCount)) {
                String msg = String.format("[%s] 总量不一致! Src:%d, Tgt:%d, Diff:%d", sourceTable, sourceCount, targetCount, sourceCount - targetCount);
                log.warn("日志记录|Log_message,table_count_mismatch,table={},srcCount={},tgtCount={},diff={}",
                        sourceTable, sourceCount, targetCount, sourceCount - targetCount);
                return CompletableFuture.completedFuture(msg);
            }
            // 3. 阶段二：逐行比对 (Keyset Paging)
            long processed = 0;
            long errors = 0;
            String lastCode = "";
            String lastDate = "1970-01-01 00:00:00";
            while (true) {
                // 3.1 查源表 (Keyset)
                List<QuotationVerificationDTO> srcBatch = dataVerificationMapper.fetchSourceBatch(sourceTable, lastCode, lastDate, BATCH_SIZE);
                if (srcBatch.isEmpty()) break;
                // 更新游标
                QuotationVerificationDTO lastRec = srcBatch.get(srcBatch.size() - 1);
                lastCode = lastRec.getWindCode();
                lastDate = lastRec.getTradeDate().toString();
                // 3.2 查目标表 (范围匹配)
                List<QuotationVerificationDTO> tgtBatch = dataVerificationMapper.fetchTargetBatchInScope(
                        targetTable, startDate, endDate,
                        srcBatch.getFirst().getWindCode(), srcBatch.getFirst().getTradeDate().toString(),
                        lastCode, lastDate);
                // 3.3 内存比对
                errors += compareBatches(sourceTable, srcBatch, tgtBatch);
                processed += srcBatch.size();
                if (processed % 100000 == 0) {
                    log.info("[{}]_进度:_{}/{}_行,_错误:_{}|Log_message", sourceTable, processed, sourceCount, errors);
                }
            }
            stopWatch.stop();
            String res = String.format("[%s] 完成. 耗时:%.1fs, 总数:%d, 错误:%d", sourceTable, stopWatch.getTotalTimeSeconds(), sourceCount, errors);
            if (errors > 0) {
                log.warn("日志记录|Log_message,table_verify_completed_with_errors,table={},elapsedSeconds={},total={},errors={}",
                        sourceTable, String.format("%.1f", stopWatch.getTotalTimeSeconds()), sourceCount, errors);
            } else {
                log.info("日志记录|Log_message,table_verify_completed,table={},elapsedSeconds={},total={},errors={}",
                        sourceTable, String.format("%.1f", stopWatch.getTotalTimeSeconds()), sourceCount, errors);
            }
            return CompletableFuture.completedFuture(res);
        } catch (Exception e) {
            log.error("[{}]_校验异常|Log_message", sourceTable, e);
            return CompletableFuture.completedFuture("异常: " + e.getMessage());
        }
    }

    /**
     * 内存比对逻辑
     */
    private long compareBatches(String table, List<QuotationVerificationDTO> srcs, List<QuotationVerificationDTO> tgts) {
        long errCount = 0;
        // List 转 Map 提速
        Map<String, QuotationVerificationDTO> tgtMap = tgts.stream().collect(Collectors.toMap(
                k -> k.getWindCode() + "_" + k.getTradeDate().toString(),
                v -> v,
                (v1, v2) -> v1 // 覆盖策略
        ));
        for (QuotationVerificationDTO s : srcs) {
            String key = s.getWindCode() + "_" + s.getTradeDate().toString();
            QuotationVerificationDTO t = tgtMap.get(key);
            if (t == null) {
                log.warn("[{}]_缺失:_Key={}", table, key);
                errCount++;
                continue;
            }
            // 字段级比对 (忽略精度差异)
            if (!isSame(s, t)) {
                log.warn("[{}]_差异:_Key={},_Src={},_Tgt={}", table, key, s, t);
                errCount++;
            }
        }
        return errCount;
    }

    private boolean isSame(QuotationVerificationDTO s, QuotationVerificationDTO t) {
        if (!cmpDec(s.getLatestPrice(), t.getLatestPrice())) return false;
        if (!cmpDec(s.getTotalVolume(), t.getTotalVolume())) return false;
        if (!cmpDec(s.getAveragePrice(), t.getAveragePrice())) return false;
        return s.getStatus().equals(t.getStatus());
    }

    private boolean cmpDec(BigDecimal a, BigDecimal b) {
        if (a == null && b == null) return true;
        if (a == null || b == null) return false;
        return a.compareTo(b) == 0;
    }
}
