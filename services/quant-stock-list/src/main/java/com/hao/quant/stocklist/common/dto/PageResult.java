package com.hao.quant.stocklist.common.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.io.Serial;
import java.io.Serializable;
import java.util.Collections;
import java.util.List;

/**
 * 分页结果通用对象。
 * <p>
 * 封装分页元数据与记录列表,便于统一响应格式。
 * </p>
 */
@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class PageResult<T> implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    private long total;
    private int pageNum;
    private int pageSize;
    private List<T> records;

    /**
     * 构建空分页结果。
     */
    public static <T> PageResult<T> empty(int pageNum, int pageSize) {
        return PageResult.<T>builder()
                .total(0)
                .pageNum(pageNum)
                .pageSize(pageSize)
                .records(Collections.emptyList())
                .build();
    }
}
