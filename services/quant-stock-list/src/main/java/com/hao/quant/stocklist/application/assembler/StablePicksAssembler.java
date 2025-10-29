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
 * <p>
 * 负责处理扩展字段的 JSON 序列化,确保下游 API 输出格式统一。
 * </p>
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class StablePicksAssembler {

    private final ObjectMapper objectMapper;

    /**
     * 将单个领域对象转换为视图对象。
     *
     * @param pick 领域模型
     * @return 视图对象
     */
    public StablePicksVO toView(StablePick pick) {
        String extraJson = null;
        if (pick.extraData() != null) {
            try {
                // 统一将扩展字段转为 JSON 字符串,方便前端展示
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

    /**
     * 批量转换领域对象集合。
     *
     * @param picks 领域模型集合
     * @return 视图对象集合
     */
    public List<StablePicksVO> toView(List<StablePick> picks) {
        return picks.stream().map(this::toView).collect(Collectors.toList());
    }
}
