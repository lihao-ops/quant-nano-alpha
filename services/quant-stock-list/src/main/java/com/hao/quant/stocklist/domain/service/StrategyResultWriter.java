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

    /**
     * 批量写入策略结果并广播更新事件。
     *
     * @param picks 待保存的策略结果集合
     */
    @Transactional(rollbackFor = Exception.class, isolation = Isolation.READ_COMMITTED, propagation = Propagation.REQUIRED)
    public void batchSave(List<StrategyDailyPickPO> picks) {
        if (picks == null || picks.isEmpty()) {
            log.warn("批量保存为空|Batch_save_empty");
            return;
        }
        // 通过 Mapper 批量写入数据库
        int inserted = stablePicksMapper.batchInsert(picks);
        log.info("批量保存成功|Batch_save_success,count={}", inserted);
        // 将所有涉及的交易日回写布隆过滤器,降低查询时穿透风险
        picks.stream().map(StrategyDailyPickPO::getTradeDate).distinct().forEach(bloomFilter::addTradeDate);
        publishEvent(picks.getFirst().getStrategyId(), picks);
    }

    /**
     * 写入单条策略结果。
     *
     * @param pick 单条数据
     */
    @Transactional(rollbackFor = Exception.class)
    public void saveOne(StrategyDailyPickPO pick) {
        // 单条写入采用普通 insert
        stablePicksMapper.insert(pick);
        bloomFilter.addTradeDate(pick.getTradeDate());
        publishEvent(pick.getStrategyId(), List.of(pick));
    }

    /**
     * 发布策略结果计算完成事件,用于通知查询侧刷新缓存。
     *
     * @param strategyId 策略标识
     * @param picks      刚写入的记录集合
     */
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
        // Kafka 异步推送事件,由查询侧监听刷新缓存
        kafkaTemplate.send("strategy.result.completed", event);
        log.info("发布策略结果事件|Publish_strategy_event,event={}", event);
    }
}
