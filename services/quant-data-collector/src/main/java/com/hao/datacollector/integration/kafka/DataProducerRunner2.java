package com.hao.datacollector.integration.kafka;

import com.hao.datacollector.cache.StockCache;
import com.hao.datacollector.dal.dao.QuotationMapper;
import com.hao.datacollector.dto.quotation.HistoryTrendDTO;
import com.hao.datacollector.service.KafkaProducerService;
import com.hao.datacollector.service.QuotationService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.CommandLineRunner;
import org.springframework.stereotype.Component;

import java.text.DecimalFormat;
import java.util.ArrayList;
import java.util.List;

@Slf4j
@Component
public class DataProducerRunner2 implements CommandLineRunner {

    @Autowired
    private KafkaProducerService producerService;

    @Autowired
    private QuotationService quotationService;

    @Autowired
    private QuotationMapper quotationMapper;

    private static final int BATCH_SIZE = 300;       // 每批发送条数，可根据实际情况调整
    private static final int PER_SECOND_TARGET = 1800; // 每秒发送条数目标

    @Override
    public void run(String... args) throws Exception {
        String topic = "quotation"; // Kafka 统一 topic
        // 取一部分股票用于单机实验
        List<String> windCodeList = StockCache.allWindCode.subList(0, 5400 / 3);
        List<HistoryTrendDTO> historyTrendDataByDate = quotationMapper
                .selectByWindCodeListAndDate(windCodeList, "2025-08-04 00:00:00", "2025-08-05 00:00:00");
        if (historyTrendDataByDate == null || historyTrendDataByDate.isEmpty()) {
            log.warn("没有获取到历史行情数据，topic={}", topic);
            return;
        }
        // 测试 3 分钟 → 3 × 60 × 1800 ≈ 324,000 条消息
        historyTrendDataByDate = historyTrendDataByDate.subList(0, Math.min(324000, historyTrendDataByDate.size()));
        log.info("开始发送历史行情数据到 Kafka topic={}，总条数={}", topic, historyTrendDataByDate.size());
        sendPerSecondWithTPS(topic, historyTrendDataByDate, BATCH_SIZE, PER_SECOND_TARGET);
    }

    /**
     * 按每秒目标条数发送，并实时打印滑动 TPS，最后输出平均 TPS 和平均耗时
     */
    private void sendPerSecondWithTPS(String topic, List<HistoryTrendDTO> historyTrendData, int batchSize, int perSecondTarget) throws InterruptedException {
        List<HistoryTrendDTO> batchList = new ArrayList<>(batchSize);
        int totalSent = 0;
        int countInCurrentSecond = 0;
        long secondStartTime = System.currentTimeMillis();
        long totalStartTime = System.currentTimeMillis();

        DecimalFormat df = new DecimalFormat("#0.00");

        for (HistoryTrendDTO dto : historyTrendData) {
            batchList.add(dto);
            countInCurrentSecond++;

            // 批量发送
            if (batchList.size() >= batchSize) {
                sendBatch(topic, batchList);
                totalSent += batchList.size();
                batchList.clear();
            }

            // 每秒发送速率控制
            if (countInCurrentSecond >= perSecondTarget) {
                long now = System.currentTimeMillis();
                long elapsed = now - secondStartTime;
                double tps = countInCurrentSecond * 1000.0 / Math.max(elapsed, 1);
                log.info("本秒发送条数={}, 耗时={}ms, 实时TPS={}, 总发送条数={}",
                        countInCurrentSecond, elapsed, df.format(tps), totalSent);

                if (elapsed < 1000) {
                    Thread.sleep(1000 - elapsed); // 等待剩余时间
                }

                secondStartTime = System.currentTimeMillis();
                countInCurrentSecond = 0;
            }
        }

        // 发送剩余不足 batchSize 的数据
        if (!batchList.isEmpty()) {
            sendBatch(topic, batchList);
            totalSent += batchList.size();

            long now = System.currentTimeMillis();
            long elapsed = now - secondStartTime;
            double tps = batchList.size() * 1000.0 / Math.max(elapsed, 1);
            log.info("最后批次发送条数={}，耗时={}ms，实时TPS={}, 总发送条数={}",
                    batchList.size(), elapsed, df.format(tps), totalSent);
        }

        long totalElapsed = System.currentTimeMillis() - totalStartTime;
        double averageTPS = totalSent * 1000.0 / Math.max(totalElapsed, 1);

        log.info("历史行情数据批量发送完成，总条数={}，总耗时={}ms，平均TPS={}", totalSent, totalElapsed, df.format(averageTPS));
    }

    /**
     * 批量发送方法
     */
    private void sendBatch(String topic, List<HistoryTrendDTO> batchData) {
        for (HistoryTrendDTO dto : batchData) {
            try {
                producerService.send(topic, dto.getWindCode(), dto);
            } catch (Exception e) {
                log.error("批量发送失败: windCode={}, error={}", dto.getWindCode(), e.getMessage(), e);
            }
        }
    }
}
