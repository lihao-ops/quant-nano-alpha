package com.hao.strategyengine.model.response;

import lombok.AllArgsConstructor;
import lombok.Data;
import java.util.List;

@Data
@AllArgsConstructor
public class StrategyResultBundle {
    private String comboKey;
    private List<StrategyResult> results;
}
