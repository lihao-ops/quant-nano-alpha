package com.hao.quant.stocklist.common.dto;

import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;

/**
 * 通用返回结果包装。
 * <p>
 * 统一 API 的响应格式,包含状态码、消息与数据。
 * </p>
 */
@Getter
@NoArgsConstructor
@AllArgsConstructor
@JsonInclude(JsonInclude.Include.NON_NULL)
public class Result<T> {

    private int code;
    private String message;
    private T data;

    /**
     * 构建成功结果。
     */
    public static <T> Result<T> success(T data) {
        return new Result<>(200, "OK", data);
    }

    /**
     * 构建无数据的成功结果。
     */
    public static Result<Void> success() {
        return new Result<>(200, "OK", null);
    }

    /**
     * 构建失败结果。
     */
    public static <T> Result<T> failure(int code, String message) {
        return new Result<>(code, message, null);
    }
}
