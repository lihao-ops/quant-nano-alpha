package com.hao.quant.stocklist.application.assembler;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.hao.quant.stocklist.application.vo.StablePicksVO;
import com.hao.quant.stocklist.domain.model.StablePick;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.stream.Collectors;

/**
 * 将领域模型转换为前端视图对象。
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class StablePicksAssembler {

    private final ObjectMapper objectMapper;

    public StablePicksVO toView(StablePick pick) {
        String extraJson = null;
        if (pick.extraData() != null) {
            try {
                extraJson = objectMapper.writeValueAsString(pick.extraData());
            } catch (JsonProcessingException e) {
                log.warn("序列化扩展字段失败: {}", e.getMessage());
            }
        }
        return StablePicksVO.builder()
                .strategyId(pick.strategyId())
                .stockCode(pick.stockCode())
                .stockName(pick.stockName())
                .industry(pick.industry())
                .score(pick.score())
                .ranking(pick.ranking())
                .marketCap(pick.marketCap())
                .peRatio(pick.peRatio())
                .tradeDate(pick.tradeDate())
                .extraData(extraJson)
                .build();
    }

    public List<StablePicksVO> toView(List<StablePick> picks) {
        return picks.stream().map(this::toView).collect(Collectors.toList());
    }
}
