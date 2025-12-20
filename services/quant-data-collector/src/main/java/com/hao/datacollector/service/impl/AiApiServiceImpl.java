package com.hao.datacollector.service.impl;

import com.hao.datacollector.service.AiApiService;
import com.openai.client.OpenAIClient;
import com.openai.errors.RateLimitException;
import com.openai.models.responses.Response;
import com.openai.models.responses.ResponseCreateParams;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

/**
 * @author hli
 * @program: quant-nano-alpha
 * @Date 2025-12-01 14:19:49
 * @description: AiApi相关实现
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class AiApiServiceImpl implements AiApiService {

    @Autowired
    private OpenAIClient openAIClient;

    @Value("${ai.openai.model}")
    private String defaultModel;

    /**
     * Call OpenAI chat API / 调用OpenAI聊天接口
     *
     * @param input user prompt / 用户输入
     * @return model reply / 模型回复
     */
    public String openAiChat(String input) {
        try {
            log.info("Calling_OpenAI_chat_|_调用OpenAI聊天接口,_model={},_input={}", defaultModel, input);
            ResponseCreateParams params = ResponseCreateParams.builder()
                    .input(input)
                    .model(defaultModel)
                    .build();

            Response response = openAIClient.responses().create(params);
            log.info("OpenAI_chat_success_|_调用成功,_output={}", response.output());
            return response.output().toString();

        } catch (RateLimitException e) {
            log.warn("OpenAI_rate_limit_hit_|_触发OpenAI限流:_{}", e.getMessage());
            return "Rate limit reached, retry later.";
        } catch (Exception e) {
            log.error("OpenAI_request_failed_|_OpenAI请求失败", e);
            return "Error while contacting OpenAI.";
        }
    }
}
