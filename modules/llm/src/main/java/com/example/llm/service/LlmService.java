package com.example.llm.service;

import reactor.core.publisher.Flux;

/**
 * LLM Service Interface
 */
public interface LlmService {

    /**
     * Stream chat with specified model
     */
    Flux<String> streamChat(String model, String message);

    /**
     * Stream chat with default model
     */
    Flux<String> streamChat(String message);

    /**
     * Chat with specified model, returns complete response
     */
    String chat(String model, String message);

    /**
     * Chat with default model
     */
    String chat(String message);
}
