package com.example.llm.service.impl;

import com.example.llm.enums.LlmModel;
import com.example.llm.service.LlmService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.chat.messages.UserMessage;
import org.springframework.ai.chat.model.ChatResponse;
import org.springframework.ai.chat.prompt.Prompt;
import org.springframework.ai.openai.OpenAiChatModel;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Flux;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * LLM Service Implementation using Spring AI
 */
@Slf4j
@Service
public class LlmServiceImpl implements LlmService {

    private final OpenAiChatModel chatModel;

    private final Map<LlmModel, String> modelEndpoints = new ConcurrentHashMap<>();

    @Value("${spring.ai.openai.chat.options.model:gpt-3.5-turbo}")
    private String defaultModel;

    @Autowired
    public LlmServiceImpl(OpenAiChatModel chatModel) {
        this.chatModel = chatModel;
        // Configure endpoints for different models (OpenAI-compatible)
        modelEndpoints.put(LlmModel.OPENAI, "gpt-3.5-turbo");
        modelEndpoints.put(LlmModel.QWEN, "qwen-turbo");
        modelEndpoints.put(LlmModel.GLM, "glm-4");
        modelEndpoints.put(LlmModel.DEEPSEEK, "deepseek-chat");
    }

    @Override
    public Flux<String> streamChat(String model, String message) {
        LlmModel llmModel = LlmModel.fromValue(model);
        return streamChat(message);
    }

    @Override
    public Flux<String> streamChat(String message) {
        log.info("Processing streaming chat request: {}", message);

        Prompt prompt = new Prompt(new UserMessage(message));

        return Flux.create(sink -> {
            chatModel.stream(prompt)
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
        return chat(message);
    }

    @Override
    public String chat(String message) {
        log.info("Processing chat request: {}", message);

        Prompt prompt = new Prompt(new UserMessage(message));
        ChatResponse response = chatModel.call(prompt);
        return extractContent(response);
    }

    private String extractContent(ChatResponse response) {
        if (response != null &&
            response.getResults() != null &&
            !response.getResults().isEmpty()) {
            return response.getResults().get(0).getOutput().getText();
        }
        return "";
    }
}
