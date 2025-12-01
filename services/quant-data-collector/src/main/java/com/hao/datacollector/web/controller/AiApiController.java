package com.hao.datacollector.web.controller;

import com.hao.datacollector.service.AiApiService;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/**
 * @author hli
 * @program: quant-nano-alpha
 * @Date 2025-12-01 14:18:41
 * @description: 模型API
 */
@Slf4j
@Tag(name = "模型API")
@RequestMapping("ai_api")
@RestController
public class AiApiController {

    @Autowired
    private AiApiService aiApiService;

    /**
     * Chat with OpenAI / 调用OpenAI聊天接口
     *
     * @param input user prompt / 用户输入
     * @return model reply / 模型回复
     */
    @GetMapping("/openai_chat")
    public String chat(@RequestParam String input) {
        log.info("Handling /openai_chat request | 处理聊天请求, input={}", input);
        String response = aiApiService.openAiChat(input);
        log.info("Completed /openai_chat request | 聊天请求完成, response={}", response);
        return response;
    }
}
