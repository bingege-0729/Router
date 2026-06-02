package com.javaee.backend.config;

import dev.langchain4j.model.openai.OpenAiChatModel;
import dev.langchain4j.model.openai.OpenAiStreamingChatModel;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.time.Duration;

@Slf4j
@Configuration
public class LLMConfig {

    @Value("${langchain4j.api-key}")
    private String apiKey;

    @Value("${langchain4j.model-name:qwen-max}")
    private String modelName;

    @Value("${langchain4j.base-url:https://api.openai.com/v1}")
    private String baseUrl;

    @Bean
    public OpenAiChatModel openAiChatModel() {
        log.info("🤖 初始化LLM模型: model={}, base_url={}, 超时=3分钟", modelName, baseUrl);

        return OpenAiChatModel.builder()
                .apiKey(apiKey)
                .modelName(modelName)
                .baseUrl(baseUrl)
                .timeout(Duration.ofSeconds(180))
                .maxRetries(2)
                .temperature(0.7)
                .build();
    }

    @Bean
    public OpenAiStreamingChatModel streamingOpenAiChatModel() {
        log.info("🌊 初始化LLM流式模型: model={}, base_url={}", modelName, baseUrl);

        return OpenAiStreamingChatModel.builder()
                .apiKey(apiKey)
                .modelName(modelName)
                .baseUrl(baseUrl)
                .timeout(Duration.ofSeconds(180))
                .temperature(0.7)
                .build();
    }
}
