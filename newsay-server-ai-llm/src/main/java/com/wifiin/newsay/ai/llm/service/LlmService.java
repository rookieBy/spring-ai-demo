package com.wifiin.newsay.ai.llm.service;

import com.wifiin.newsay.ai.llm.model.StreamChunk;
import org.springframework.http.codec.ServerSentEvent;
import reactor.core.publisher.Flux;

/**
 * LLM Service Interface
 */
public interface LlmService {

    /**
     * Stream chat with conversation memory support
     * @param model The LLM model to use
     * @param message The user message
     * @param conversationId The conversation ID for memory (can be null)
     */
    Flux<String> streamChat(String model, String message, String conversationId);

    /**
     * Stream chat without conversation memory (convenience method)
     */
    Flux<String> streamChat(String model, String message);

    /**
     * Stream chat with default model and conversation memory
     */
    Flux<String> streamChat(String message);

    /**
     * Chat with conversation memory support
     * @param model The LLM model to use
     * @param message The user message
     * @param conversationId The conversation ID for memory (can be null)
     */
    String chat(String model, String message, String conversationId);

    /**
     * Chat without conversation memory (convenience method)
     */
    String chat(String model, String message);

    /**
     * Chat with default model
     */
    String chat(String message);


    Flux<ServerSentEvent<StreamChunk>> smartStream(String model, String message);

    String mcpSearch(String message);
}
