package com.hao.strategyengine.common.util;

import org.springframework.util.DigestUtils;
import java.util.List;

/**
 * KeyUtils 工具类
 *
 * <p>提供生成策略相关的唯一 Key 的方法，例如将多个策略 ID 拼接并生成 MD5 哈希值。</p>
 *
 * <p>用途示例：</p>
 * <pre>{@code
 * List<String> strategyIds = Arrays.asList("strategy1", "strategy2");
 * String key = KeyUtils.comboKey(strategyIds);
 * }</pre>
 *
 * <p>注意：该方法使用 MD5 生成摘要，不保证加密安全性，仅用于生成唯一标识。</p>
 *
 * @author hli
 * @date 2025-10-22
 */
public class KeyUtils {

    /**
     * 将策略 ID 列表组合为一个唯一 Key
     *
     * @param strategyIds 策略 ID 列表
     * @return 拼接后的字符串的 MD5 值，作为唯一 Key
     */
    public static String comboKey(List<String> strategyIds) {
        if (strategyIds == null || strategyIds.isEmpty()) {
            throw new IllegalArgumentException("strategyIds cannot be null or empty");
        }
        // 用逗号拼接策略ID
        String joined = String.join(",", strategyIds);
        // 返回 MD5 摘要
        return DigestUtils.md5DigestAsHex(joined.getBytes());
    }
}
