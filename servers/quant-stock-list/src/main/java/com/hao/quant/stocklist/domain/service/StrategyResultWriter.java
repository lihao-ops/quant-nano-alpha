package com.hao.quant.stocklist.domain.service;

import com.hao.quant.stocklist.domain.bloom.StablePicksBloomFilter;
import com.hao.quant.stocklist.domain.event.StrategyResultEvent;
import com.hao.quant.stocklist.infrastructure.persistence.mapper.StablePicksMapper;
import com.hao.quant.stocklist.infrastructure.persistence.po.StrategyDailyPickPO;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Isolation;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

/**
 * 写入侧服务,用于批量落库策略结果并发出刷新事件。
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class StrategyResultWriter {

    private final StablePicksMapper stablePicksMapper;
    private final KafkaTemplate<String, Object> kafkaTemplate;
    private final StablePicksBloomFilter bloomFilter;

    @Transactional(rollbackFor = Exception.class, isolation = Isolation.READ_COMMITTED, propagation = Propagation.REQUIRED)
    public void batchSave(List<StrategyDailyPickPO> picks) {
        if (picks == null || picks.isEmpty()) {
            log.warn("批量保存策略结果数据为空");
            return;
        }
        int inserted = stablePicksMapper.batchInsert(picks);
        log.info("批量保存策略结果成功,共{}条", inserted);
        picks.stream().map(StrategyDailyPickPO::getTradeDate).distinct().forEach(bloomFilter::addTradeDate);
        publishEvent(picks.getFirst().getStrategyId(), picks);
    }

    @Transactional(rollbackFor = Exception.class)
    public void saveOne(StrategyDailyPickPO pick) {
        stablePicksMapper.insert(pick);
        bloomFilter.addTradeDate(pick.getTradeDate());
        publishEvent(pick.getStrategyId(), List.of(pick));
    }

    private void publishEvent(String strategyId, List<StrategyDailyPickPO> picks) {
        LocalDate tradeDate = picks.getFirst().getTradeDate();
        StrategyResultEvent event = StrategyResultEvent.builder()
                .eventId(UUID.randomUUID().toString())
                .tradeDate(tradeDate)
                .strategyId(strategyId)
                .version(picks.getFirst().getVersion())
                .stockCount(picks.size())
                .eventTime(LocalDateTime.now())
                .source("STABLE-PICKS-WRITER")
                .build();
        kafkaTemplate.send("strategy.result.completed", event);
        log.info("发布策略结果事件: {}", event);
    }
}
