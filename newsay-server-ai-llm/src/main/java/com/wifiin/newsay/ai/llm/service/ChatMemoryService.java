package com.wifiin.newsay.ai.llm.service;

import org.springframework.ai.chat.messages.Message;

import java.util.List;

/**
 * Chat Memory Service Interface
 * Manages conversation history with sliding window
 */
public interface ChatMemoryService {

    /**
     * Get conversation history for a given conversationId
     * @param conversationId the conversation identifier
     * @return list of messages in chronological order
     */
    List<Message> getHistory(String conversationId);

    /**
     * Add a user message to conversation history
     * @param conversationId the conversation identifier
     * @param userMessage the user message content
     */
    void addUserMessage(String conversationId, String userMessage);

    /**
     * Add an assistant message to conversation history
     * @param conversationId the conversation identifier
     * @param assistantMessage the assistant message content
     */
    void addAssistantMessage(String conversationId, String assistantMessage);

    /**
     * Clear conversation history
     * @param conversationId the conversation identifier
     */
    void clear(String conversationId);
}
