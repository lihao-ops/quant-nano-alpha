package com.hao.datacollector.web.vo.limitup;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.io.Serializable;

@Data
@Schema(name = "ApiResponse")
public class ApiResponse<T> implements Serializable {
    private static final long serialVersionUID = 1L;

    @Schema(description = "")
    private String resultCode;
    @Schema(description = "")
    private T resultObject;
    @Schema(description = "")
    private String returnTime;

    public String getResultCode() {
        return resultCode;
    }
}