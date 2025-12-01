package com.hao.datacollector.service;

public interface AiApiService {

    /**
     * Send a prompt to OpenAI and get the reply / 发送提示词给OpenAI获取回复
     *
     * @param input user prompt / 用户输入
     * @return model reply / 模型回复
     */
    String openAiChat(String input);
}
