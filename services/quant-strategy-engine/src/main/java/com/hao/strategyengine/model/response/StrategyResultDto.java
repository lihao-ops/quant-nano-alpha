package com.hao.strategyengine.model.response;

import lombok.Builder;
import lombok.Data;
import java.util.List;

@Data
@Builder
public class StrategyResultDto {
    private String comboKey;
    private List<Object> results;
}
