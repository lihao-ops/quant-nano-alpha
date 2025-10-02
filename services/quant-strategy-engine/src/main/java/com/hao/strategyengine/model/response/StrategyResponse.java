package com.hao.strategyengine.model.response;

import lombok.Data;

@Data
public class StrategyResponse {
    private boolean success;
    private String message;
    private Object data;

    public static StrategyResponse success(String message) {
        StrategyResponse response = new StrategyResponse();
        response.setSuccess(true);
        response.setMessage(message);
        return response;
    }

    public static StrategyResponse error(String message) {
        StrategyResponse response = new StrategyResponse();
        response.setSuccess(false);
        response.setMessage(message);
        return response;
    }
}