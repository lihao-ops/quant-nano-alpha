package com.hao.strategyengine.api.controller;

import com.hao.strategyengine.core.facade.StrategyEngineFacade;
import com.hao.strategyengine.integration.kafka.KafkaConsumerConfig;
import com.hao.strategyengine.model.core.StrategyContext;
import com.hao.strategyengine.model.request.StrategyRequest;
import com.hao.strategyengine.model.response.StrategyResultBundle;
import lombok.RequiredArgsConstructor;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import java.time.Instant;
import java.util.concurrent.CompletableFuture;

/**
 * ===============================================================
 * 【类名】：StrategyController（策略执行入口层）
 * ===============================================================
 *
 * 【功能定位】：
 *   ⦿ 提供对外 HTTP 接口，作为策略引擎的统一入口；
 *   ⦿ 接收用户请求（策略组合、标的、附加参数）；
 *   ⦿ 封装上下文（StrategyContext）并调用 Facade 统一调度；
 *   ⦿ 通过 Server-Sent Events (SSE) 实时推送计算结果；
 *   ⦿ 异步推送计算结果到 Kafka 消息队列。
 *
 * 【核心思路】：
 *   - Controller 不直接参与业务计算，只负责协调调用；
 *   - 计算逻辑交由 Facade + 分布式锁 + Dispatcher 完成；
 *   - 结果流式返回，避免长连接阻塞；
 *   - SSE 适合策略计算类长耗时接口。
 *
 * 【执行链位置】：
 *   ✅ 属于系统调用链的「第 1 层」：
 *   Controller(第1层) → Service/Facade(第2~3层) → Lock(第4层)
 *   → Dispatcher(第5层) → Strategy(第6层)
 *
 * 【执行流程】：
 *   ┌───────────────────────────────────────────┐
 *   │ Step 1：接收外部 POST 请求 (/api/strategy/execute) │
 *   │ Step 2：封装请求参数为 StrategyContext              │
 *   │ Step 3：调用 Facade 执行策略组合（分布式锁保护）     │
 *   │ Step 4：获取计算结果 StrategyResultBundle           │
 *   │ Step 5：推送结果到 Kafka 和 SSE 客户端               │
 *   └───────────────────────────────────────────┘
 *
 * 【响应机制】：
 *   - 返回类型为 SseEmitter：服务端实时推送结果；
 *   - 客户端可使用 EventSource 监听返回；
 *   - 适用于“策略执行、回测、信号监控”等流式任务。
 */
@RestController
@RequiredArgsConstructor
@RequestMapping("/api/strategy")
public class StrategyController {

    /** 外观层：封装锁、分发、缓存、Kafka 的统一执行入口 */
    private final StrategyEngineFacade engine;

    /** Kafka 发布配置类，用于异步广播计算结果 */
    private final KafkaConsumerConfig kafkaPublisher;

    /**
     * ===============================================================
     * 【方法名】：execute
     * ===============================================================
     *
     * 【功能】：
     *   接收策略执行请求并异步返回结果。
     *   使用 SSE 实现实时响应流，避免前端超时。
     *
     * 【参数】：
     *   @param req  策略执行请求体（包含 userId、策略ID集合、symbol、额外参数）
     *
     * 【返回】：
     *   SseEmitter（服务端事件流，推送结果或异常）
     *
     * 【执行流程】：
     *   ① 创建 SseEmitter（30 秒超时）；
     *   ② 异步线程构建 StrategyContext；
     *   ③ 调用 Facade 执行所有策略；
     *   ④ 推送结果到 Kafka；
     *   ⑤ 发送 SSE 响应流；
     *   ⑥ 处理异常情况。
     */
    @PostMapping(value = "/execute", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    public SseEmitter execute(@RequestBody StrategyRequest req) {
        // Step 1️⃣ 创建 SSE 流，允许 30 秒超时
        SseEmitter emitter = new SseEmitter(30_000L);

        // Step 2️⃣ 异步执行任务，防止 Controller 阻塞
        CompletableFuture.runAsync(() -> {
            try {
                // Step 3️⃣ 构建策略上下文对象（封装调用环境）
                StrategyContext ctx = StrategyContext.builder()
                        .userId(req.getUserId())
                        .symbol(req.getSymbol())
                        .extra(req.getExtra())
                        .requestTime(Instant.now())
                        .build();

                // Step 4️⃣ 调用 Facade 执行策略组合（内部含锁/并发控制）
                StrategyResultBundle bundle =
                        engine.executeAll(req.getUserId(), req.getStrategyIds(), ctx);

                // Step 5️⃣ 异步发布 Kafka 消息（非阻塞）
                kafkaPublisher.publish("quant-strategy-result", bundle);

                // Step 6️⃣ SSE 推送执行结果给前端
                emitter.send(bundle);

                // Step 7️⃣ 完成推送并关闭连接
                emitter.complete();

            } catch (Exception e) {
                // Step 8️⃣ 异常处理，推送错误并结束 SSE
                try {
                    emitter.send(SseEmitter.event()
                            .name("error")
                            .data("执行异常：" + e.getMessage()));
                } catch (Exception ignored) {}
                emitter.completeWithError(e);
            }
        });

        return emitter;
    }
}
