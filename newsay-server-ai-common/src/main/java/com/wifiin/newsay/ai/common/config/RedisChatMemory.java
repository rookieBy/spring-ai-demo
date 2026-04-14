package com.wifiin.newsay.ai.common.config;

import org.springframework.ai.chat.messages.Message;
import org.springframework.ai.chat.memory.ChatMemory;

import java.util.ArrayList;
import java.util.List;

/**
 * Redis-based ChatMemory implementation with sliding window
 * Keeps only the most recent N messages per conversation
 */
public class RedisChatMemory implements ChatMemory {

    private final RedisChatMemoryRepository repository;
    private final int maxMessages;

    public RedisChatMemory(RedisChatMemoryRepository repository) {
        this(repository, 10);
    }

    public RedisChatMemory(RedisChatMemoryRepository repository, int maxMessages) {
        this.repository = repository;
        this.maxMessages = maxMessages;
    }

    @Override
    public void add(String conversationId, Message message) {
        List<Message> messages = repository.findByConversationId(conversationId);
        messages.add(message);

        // Apply sliding window - keep only the most recent messages
        if (messages.size() > maxMessages) {
            // Calculate how many messages to remove from the beginning
            int excess = messages.size() - maxMessages;
            messages = new ArrayList<>(messages.subList(excess, messages.size()));
        }

        repository.saveAll(conversationId, messages);
    }

    @Override
    public void add(String conversationId, List<Message> messages) {
        List<Message> existingMessages = repository.findByConversationId(conversationId);
        existingMessages.addAll(messages);

        // Apply sliding window - keep only the most recent messages
        if (existingMessages.size() > maxMessages) {
            int excess = existingMessages.size() - maxMessages;
            existingMessages = new ArrayList<>(existingMessages.subList(excess, existingMessages.size()));
        }

        repository.saveAll(conversationId, existingMessages);
    }

    @Override
    public List<Message> get(String conversationId) {
        return repository.findByConversationId(conversationId);
    }

    @Override
    public void clear(String conversationId) {
        repository.deleteByConversationId(conversationId);
    }
}
