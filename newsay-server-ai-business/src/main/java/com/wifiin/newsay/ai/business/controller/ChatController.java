package com.wifiin.newsay.ai.business.controller;

import com.wifiin.newsay.ai.api.dto.ChatRequest;
import com.wifiin.newsay.ai.common.result.Result;
import com.wifiin.newsay.ai.llm.model.StreamChunk;
import com.wifiin.newsay.ai.llm.service.LlmService;
import jakarta.validation.Valid;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.MediaType;
import org.springframework.http.codec.ServerSentEvent;
import org.springframework.web.bind.annotation.*;
import reactor.core.publisher.Flux;

import java.util.Map;

/**
 * Chat Controller - Handles chat requests to various LLMs
 * 放在 business 模块，因为这是业务入口
 */
@RestController
@RequestMapping("/api/chat")
public class ChatController {

    private static final Logger log = LoggerFactory.getLogger(ChatController.class);

    private final LlmService llmService;

    public ChatController(LlmService llmService) {
        this.llmService = llmService;
    }

    /**
     * Streaming chat endpoint - returns SSE stream
     */
    @PostMapping(value = "/stream", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    public Flux<String> streamChat(@Valid @RequestBody ChatRequest request) {
        log.info("Received streaming chat request - model: {}, message: {}, conversationId: {}",
                request.getModel(), request.getMessage(), request.getConversationId());

        String model = request.getModel() != null ? request.getModel() : "deepseek";
        return llmService.streamChat(model, request.getMessage(), request.getConversationId())
                .map(content -> {
                    log.warn("Streaming content: {}", content);
                    return content;
                });
    }

    /**
     * Streaming chat endpoint - returns SSE  markdown
     */
    @PostMapping(value = "/stream/markdown", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    public Flux<ServerSentEvent<StreamChunk>> smartStream(@Valid @RequestBody ChatRequest request) {
        log.info("Received streaming chat request - model: {}, message: {}",
                request.getModel(), request.getMessage());

        String model = request.getModel() != null ? request.getModel() : "deepseek";
        return llmService.smartStream(model, request.getMessage());
    }

    @GetMapping(value = "/thread-info", produces = MediaType.APPLICATION_JSON_VALUE)
    public Map<String, Object> testThread() {
        Thread thread = Thread.currentThread();
        return Map.of("threadName", thread.getName(), "isVirtual", thread.isVirtual(), "threadClass", thread.getClass().getName());
    }

    /**
     * Non-streaming chat endpoint
     */
    @PostMapping
    public Result<String> chat(@Valid @RequestBody ChatRequest request) {
        log.info("Received chat request - model: {}, message: {}, conversationId: {}",
                request.getModel(), request.getMessage(), request.getConversationId());

        String model = request.getModel() != null ? request.getModel() : "qwen";
        String response = llmService.chat(model, request.getMessage(), request.getConversationId());
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

    @GetMapping("/search")
    public String search(@RequestParam("q") String question) {
        return llmService.mcpSearch(question);
    }

}
