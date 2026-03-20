package com.example.launcher.controller;

import com.example.api.dto.ChatRequest;
import com.example.common.result.Result;
import com.example.llm.service.LlmService;
import jakarta.validation.Valid;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.*;
import reactor.core.publisher.Flux;

import java.util.Map;

/**
 * LLM Controller - Handles chat requests to various LLMs
 */
@Slf4j
@RestController
@RequestMapping("/api/llm")
public class LlmController {

    private final LlmService llmService;

    public LlmController(LlmService llmService) {
        this.llmService = llmService;
    }

    /**
     * Streaming chat endpoint - returns SSE stream
     */
    @PostMapping(value = "/chat/stream", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    public Flux<String> streamChat(@Valid @RequestBody ChatRequest request) {
        log.info("Received streaming chat request - model: {}, message: {}",
                request.getModel(), request.getMessage());

        String model = request.getModel() != null ? request.getModel() : "qwen";
        return llmService.streamChat(model, request.getMessage())
                .map(content -> {
                    log.debug("Streaming content: {}", content);
                    return "data: " + content + "\n\n";
                });
    }

    /**
     * Non-streaming chat endpoint
     */
    @PostMapping("/chat")
    public Result<String> chat(@Valid @RequestBody ChatRequest request) {
        log.info("Received chat request - model: {}, message: {}",
                request.getModel(), request.getMessage());

        String model = request.getModel() != null ? request.getModel() : "qwen";
        String response = llmService.chat(model, request.getMessage());
        return Result.success(response);
    }

    /**
     * Get supported models
     */
    @GetMapping("/models")
    public Result<Map<String, String>> getSupportedModels() {
        return Result.success(Map.of(
                "qwen", "Alibaba Qwen",
                "glm", "Zhipu GLM",
                "deepseek", "DeepSeek",
                "openai", "OpenAI GPT"
        ));
    }
}
