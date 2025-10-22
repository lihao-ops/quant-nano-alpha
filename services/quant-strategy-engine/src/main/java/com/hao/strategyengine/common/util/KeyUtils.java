package com.hao.strategyengine.common.util;

/**
 * @author hli
 * @program: quant-nano-alpha
 * @Date 2025-10-22 20:11:31
 * @description:
 */

import org.springframework.util.DigestUtils;

import java.util.List;

public class KeyUtils {
    public static String comboKey(List strategyIds) {
        String joined = String.join(",", strategyIds);
        return DigestUtils.md5DigestAsHex(joined.getBytes());
    }
}