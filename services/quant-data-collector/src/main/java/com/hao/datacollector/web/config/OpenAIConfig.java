package com.hao.datacollector.web.config;

import com.openai.client.OpenAIClient;
import com.openai.client.okhttp.OpenAIOkHttpClient;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.net.InetSocketAddress;
import java.net.Proxy;

@Slf4j
@Configuration
public class OpenAIConfig {

    @Value("${ai.openai.api-key}")
    private String apiKey;

    @Value("${ai.proxy.host}")
    private String proxyHost;

    @Value("${ai.proxy.port}")
    private int proxyPort;

    @Bean
    public OpenAIClient openAIClient() {

        Proxy proxy = new Proxy(Proxy.Type.HTTP, new InetSocketAddress(proxyHost, proxyPort));

        log.info("🔧 Initializing OpenAI Client (Proxy: {}:{})", proxyHost, proxyPort);

        return OpenAIOkHttpClient.builder()
                .proxy(proxy)
                .apiKey(apiKey)
                .build();
    }
}
