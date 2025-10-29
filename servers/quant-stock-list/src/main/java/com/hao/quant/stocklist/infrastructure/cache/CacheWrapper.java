package com.hao.quant.stocklist.infrastructure.cache;

import com.fasterxml.jackson.annotation.JsonFormat;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serial;
import java.io.Serializable;
import java.time.Duration;
import java.time.LocalDateTime;

/**
 * 缓存包装器,支持硬过期与软过期时间。
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class CacheWrapper<T> implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    private T data;

    @JsonFormat(pattern = "yyyy-MM-dd'T'HH:mm:ss")
    private LocalDateTime expireTime;

    @JsonFormat(pattern = "yyyy-MM-dd'T'HH:mm:ss")
    private LocalDateTime softExpireTime;

    public static <T> CacheWrapper<T> of(T data, Duration ttl) {
        LocalDateTime now = LocalDateTime.now();
        return new CacheWrapper<>(data, now.plus(ttl), now.plus(ttl.dividedBy(2)));
    }

    public boolean isExpired() {
        return expireTime != null && LocalDateTime.now().isAfter(expireTime);
    }

    public boolean shouldRefreshAsync() {
        if (softExpireTime == null || expireTime == null) {
            return false;
        }
        LocalDateTime now = LocalDateTime.now();
        return now.isAfter(softExpireTime) && now.isBefore(expireTime);
    }
}
