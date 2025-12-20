package com.hao.strategyengine.chain;

import com.hao.strategyengine.common.model.core.StrategyContext;
import com.hao.strategyengine.core.StrategyHandler;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ResponseStatusException;

/**
 * 鉴权处理器
 *
 * 设计目的：
 * 1. 在策略执行前进行用户鉴权校验。
 * 2. 拦截非法请求并保证责任链安全性。
 *
 * 为什么需要该类：
 * - 鉴权属于基础安全能力，需要在责任链前置统一处理。
 *
 * 核心实现思路：
 * - 校验用户是否存在并按需扩展权限校验。
 */
@Slf4j
@Component
public class AuthHandler implements StrategyHandler {
    /**
     * 执行鉴权检查
     *
     * 实现逻辑：
     * 1. 校验用户是否存在。
     * 2. 预留权限校验扩展点。
     *
     * @param ctx 策略上下文
     */
    @Override
    public void handle(StrategyContext ctx) {
        // 实现思路：
        // 1. 校验用户身份并拒绝非法访问。
        log.info("鉴权检查|Auth_check,userId={}", ctx.getUserId());
        // 第一步：校验当前用户是否有调用权限
        if (ctx.getUserId() == null) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "未登录或登录信息失效");
        }
        // 待办：后续扩展权限不足场景
//        if (!hasPermission(ctx.getUserId())) {
//            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "无访问权限");
//        }
        // 第二步：鉴权通过后放行到责任链下一个处理器
    }
}
