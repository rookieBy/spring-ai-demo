package com.wifiin.newsay.ai.llm.service;

import com.wifiin.newsay.ai.llm.model.StreamChunk;
import org.springframework.http.codec.ServerSentEvent;
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
     * Stream chat with specified model and conversationId for multi-turn conversation
     */
    Flux<String> streamChat(String model, String message, String conversationId);

    /**
     * Stream chat with search augmentation - uses MCP to search, then answers with specified model
     */
    Flux<String> streamChatWithSearch(String model, String message, String conversationId);

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


    Flux<ServerSentEvent<StreamChunk>> smartStream(String model, String message);

    String mcpSearch(String message);
}
