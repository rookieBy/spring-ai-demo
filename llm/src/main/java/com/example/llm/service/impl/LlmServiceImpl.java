package com.example.llm.service.impl;

import com.example.llm.enums.LlmModel;
import com.example.llm.service.LlmService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.chat.messages.UserMessage;
import org.springframework.ai.chat.model.ChatResponse;
import org.springframework.ai.chat.prompt.Prompt;
import org.springframework.ai.openai.OpenAiChatModel;
import org.springframework.ai.openai.OpenAiChatOptions;
import org.springframework.ai.openai.api.OpenAiApi;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Flux;

/**
 * LLM Service Implementation supporting multiple LLM providers via OpenAI-compatible API
 * Supports: DeepSeek, GLM (Zhipu), Qwen (Alibaba), OpenAI
 *
 * All models use OpenAI-compatible endpoints with different base URLs:
 * - DeepSeek: https://api.deepseek.com
 * - GLM: https://open.bigmodel.cn/api/paas/v4
 * - Qwen: https://dashscope.aliyuncs.com/compatible-mode/v1
 * - OpenAI: https://api.openai.com/v1
 */
@Slf4j
@Service
public class LlmServiceImpl implements LlmService {

    @Value("${spring.ai.openai.api-key}")
    private String openAiApiKey;

    @Value("${spring.ai.openai.base-url}")
    private String openAiBaseUrl;

    @Value("${spring.ai.deepseek.api-key:}")
    private String deepSeekApiKey;

    @Value("${spring.ai.deepseek.base-url:https://api.deepseek.com}")
    private String deepSeekBaseUrl;

    @Value("${spring.ai.alibaba.api-key:}")
    private String glmApiKey;

    @Value("${spring.ai.alibaba.base-url:https://open.bigmodel.cn/api/paas/v4}")
    private String glmBaseUrl;

    @Value("${spring.ai.dashscope.api-key:}")
    private String dashScopeApiKey;

    @Value("${spring.ai.dashscope.base-url:https://dashscope.aliyuncs.com/compatible-mode/v1}")
    private String dashScopeBaseUrl;

    private final OpenAiApi openAiApi;

    @Autowired(required = false)
    public LlmServiceImpl(OpenAiApi openAiApi) {
        this.openAiApi = openAiApi;
    }

    public LlmServiceImpl() {
        this.openAiApi = null;
    }

    @Override
    public Flux<String> streamChat(String model, String message) {
        LlmModel llmModel = LlmModel.fromValue(model);
        log.info("Processing streaming chat request with model: {}, message: {}", model, message);

        String finalApiKey = getApiKeyForModel(llmModel);
        String finalBaseUrl = getBaseUrlForModel(llmModel);
        String modelName = getModelNameForModel(llmModel);

        log.debug("Using API key from: {}, base URL: {}, model: {}", getKeySource(llmModel), finalBaseUrl, modelName);

        return streamChat(message, finalApiKey, finalBaseUrl, modelName);
    }

    @Override
    public Flux<String> streamChat(String message) {
        return streamChat("deepseek", message);
    }

    private Flux<String> streamChat(String message, String apiKey, String baseUrl, String model) {
        OpenAiApi tempApi = OpenAiApi.builder()
                .apiKey(apiKey)
                .baseUrl(baseUrl)
                .build();

        OpenAiChatModel tempChatModel = OpenAiChatModel.builder()
                .openAiApi(tempApi)
                .defaultOptions(OpenAiChatOptions.builder()
                        .model(model)
                        .temperature(0.7)
                        .build())
                .build();

        Prompt prompt = new Prompt(new UserMessage(message));

        return Flux.create(sink -> {
            tempChatModel.stream(prompt)
                    .subscribe(
                            response -> {
                                String content = extractContent(response);
                                if (content != null && !content.isEmpty()) {
                                    sink.next(content);
                                }
                            },
                            sink::error,
                            sink::complete
                    );
        });
    }

    @Override
    public String chat(String model, String message) {
        LlmModel llmModel = LlmModel.fromValue(model);
        log.info("Processing chat request with model: {}, message: {}", model, message);

        String finalApiKey = getApiKeyForModel(llmModel);
        String finalBaseUrl = getBaseUrlForModel(llmModel);
        String modelName = getModelNameForModel(llmModel);

        log.debug("Using API key from: {}, base URL: {}, model: {}", getKeySource(llmModel), finalBaseUrl, modelName);

        return chat(message, finalApiKey, finalBaseUrl, modelName);
    }

    @Override
    public String chat(String message) {
        return chat("deepseek", message);
    }

    private String chat(String message, String apiKey, String baseUrl, String model) {
        OpenAiApi tempApi = OpenAiApi.builder()
                .apiKey(apiKey)
                .baseUrl(baseUrl)
                .build();

        OpenAiChatModel tempChatModel = OpenAiChatModel.builder()
                .openAiApi(tempApi)
                .defaultOptions(OpenAiChatOptions.builder()
                        .model(model)
                        .temperature(0.7)
                        .build())
                .build();

        Prompt prompt = new Prompt(new UserMessage(message));
        ChatResponse response = tempChatModel.call(prompt);
        return extractContent(response);
    }

    private String getApiKeyForModel(LlmModel model) {
        return switch (model) {
            case DEEPSEEK -> deepSeekApiKey != null && !deepSeekApiKey.isEmpty()
                    ? deepSeekApiKey : openAiApiKey;
            case GLM -> glmApiKey != null && !glmApiKey.isEmpty()
                    ? glmApiKey : openAiApiKey;
            case QWEN -> dashScopeApiKey != null && !dashScopeApiKey.isEmpty()
                    ? dashScopeApiKey : openAiApiKey;
            case OPENAI -> openAiApiKey;
        };
    }

    private String getBaseUrlForModel(LlmModel model) {
        return switch (model) {
            case DEEPSEEK -> deepSeekBaseUrl;
            case GLM -> glmBaseUrl;
            case QWEN -> dashScopeBaseUrl;
            case OPENAI -> openAiBaseUrl;
        };
    }

    private String getModelNameForModel(LlmModel model) {
        return switch (model) {
            case DEEPSEEK -> "deepseek-chat";
            case GLM -> "glm-4";
            case QWEN -> "qwen-turbo";
            case OPENAI -> "gpt-3.5-turbo";
        };
    }

    private String getKeySource(LlmModel model) {
        return switch (model) {
            case DEEPSEEK -> "deepseek";
            case GLM -> "glm";
            case QWEN -> "dashscope";
            case OPENAI -> "openai";
        };
    }

    private String extractContent(ChatResponse response) {
        if (response != null &&
            response.getResults() != null &&
            !response.getResults().isEmpty()) {
            String text = response.getResults().get(0).getOutput().getText();
            return filterThinkingProcess(text);
        }
        return "";
    }

    /**
     * 过滤 DeepSeek 思考过程标签
     * DeepSeek 模型会在响应中包含 <think>...</think> 标签，需要过滤掉
     */
    private String filterThinkingProcess(String text) {
        if (text == null || text.isEmpty()) {
            return text;
        }
        return text.replaceAll("<think>[\\s\\S]*?</think>", "")
                   .replaceAll("\\[\\[Final Answer\\]\\]", "")
                   .trim();
    }
}
