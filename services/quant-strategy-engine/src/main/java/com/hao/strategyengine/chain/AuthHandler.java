package com.hao.strategyengine.chain;

import com.hao.strategyengine.common.model.core.StrategyContext;
import com.hao.strategyengine.core.StrategyHandler;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ResponseStatusException;

/**
 * @author hli
 * @program: quant-nano-alpha
 * @Date 2025-10-23 20:08:08
 * @description: 鉴权
 */
@Slf4j
@Component
public class AuthHandler implements StrategyHandler {
    @Override
    public void handle(StrategyContext ctx) {
        log.info("鉴权检查：userId={}", ctx.getUserId());
        // Step 1️⃣ 校验当前userId是否存在本功能接口调用权限
        if (ctx.getUserId() == null) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "未登录或登录信息失效");
        }
        //todo 未来扩展：权限不足
//        if (!hasPermission(ctx.getUserId())) {
//            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "无访问权限");
//        }
        // Step 3️⃣ 否则放行（责任链继续传递给下一个 Handler）
    }
}
