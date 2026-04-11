package com.wifiin.newsay.ai.llm.service;

import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.openai.OpenAiChatModel;
import org.springframework.ai.openai.OpenAiChatOptions;
import org.springframework.ai.openai.api.OpenAiApi;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

@Configuration
public class LLMConfig {

    @Autowired
    private LLMProperties properties;

    private ChatClient createClient(String providerName) {
        LLMProperties.ProviderConfig cfg = properties.getProvider(providerName);
        System.out.println("configMap:" + cfg);

        // 防御性编程：配置缺失时抛出明确异常
        if (cfg == null) {
            throw new IllegalStateException(
                    "Provider '" + providerName + "' not configured. " +
                            "Available: "
            );
        }

        if (!cfg.isValid()) {
            throw new IllegalStateException(
                    "Provider '" + providerName + "' config invalid. " +
                            "baseUrl=" + cfg.getBaseUrl() + ", apiKey=" + (cfg.getApiKey() != null ? "***" : "null")
            );
        }

        var openAiApi = new OpenAiApi(cfg.getBaseUrl(), cfg.getApiKey());
        var chatModel = new OpenAiChatModel(openAiApi);

        return ChatClient.builder(chatModel)
                .defaultOptions(OpenAiChatOptions.builder()
                        .model(cfg.getModel())
                        .temperature(cfg.getTemperature() != null ? cfg.getTemperature() : 0.7)
                        .build())
                .build();
    }

    @Bean("deepseekChatClient")
    public ChatClient deepseekChatClient() {
        return createClient("deepseek");
    }

    @Bean("qwenChatClient")
    public ChatClient qwenChatClient() {
        return createClient("qwen");
    }

    @Bean("glmChatClient")
    public ChatClient glmChatClient() {
        return createClient("glm");
    }

    @Bean("llmRouter")
    public Map<String, ChatClient> chatClientRouter(
            @Qualifier("deepseekChatClient") ObjectProvider<ChatClient> deepseekProvider,
            @Qualifier("qwenChatClient") ObjectProvider<ChatClient> qwenProvider,
            @Qualifier("glmChatClient") ObjectProvider<ChatClient> glmProvider) {

        Map<String, ChatClient> router = new HashMap<>();

        // 只有实际创建的 Bean 才加入路由
        deepseekProvider.ifAvailable(client -> router.put("deepseek", client));
        qwenProvider.ifAvailable(client -> router.put("qwen", client));
        glmProvider.ifAvailable(client -> router.put("glm", client));

        if (router.isEmpty()) {
            throw new IllegalStateException("No LLM provider configured!");
        }
        System.out.println("=============" + router);
        return Collections.unmodifiableMap(router);
    }
}
