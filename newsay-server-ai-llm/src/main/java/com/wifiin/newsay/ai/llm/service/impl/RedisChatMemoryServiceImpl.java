package com.wifiin.newsay.ai.llm.service.impl;

import com.wifiin.newsay.ai.llm.service.ChatMemoryService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.ai.chat.messages.AssistantMessage;
import org.springframework.ai.chat.messages.Message;
import org.springframework.ai.chat.messages.UserMessage;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * Redis-based Chat Memory Implementation with Sliding Window
 */
@Service
public class RedisChatMemoryServiceImpl implements ChatMemoryService {

    private static final Logger log = LoggerFactory.getLogger(RedisChatMemoryServiceImpl.class);
    private static final String KEY_PREFIX = "chat:memory:";

    private final StringRedisTemplate redisTemplate;
    private final int slidingWindowSize;

    public RedisChatMemoryServiceImpl(
            StringRedisTemplate redisTemplate,
            @Value("${spring.ai.chat.memory.sliding-window-size:20}") int slidingWindowSize) {
        this.redisTemplate = redisTemplate;
        this.slidingWindowSize = slidingWindowSize;
    }

    @Override
    public List<Message> getHistory(String conversationId) {
        String key = KEY_PREFIX + conversationId;
        List<String> messages = redisTemplate.opsForList().range(key, 0, -1);

        if (messages == null || messages.isEmpty()) {
            return Collections.emptyList();
        }

        List<Message> result = new ArrayList<>();
        for (String msgJson : messages) {
            result.add(parseMessage(msgJson));
        }
        return result;
    }

    @Override
    public void addUserMessage(String conversationId, String userMessage) {
        String key = KEY_PREFIX + conversationId;
        String msgJson = toJson("user", userMessage);

        redisTemplate.opsForList().rightPush(key, msgJson);
        trimToWindowSize(key);
    }

    @Override
    public void addAssistantMessage(String conversationId, String assistantMessage) {
        String key = KEY_PREFIX + conversationId;
        String msgJson = toJson("assistant", assistantMessage);

        redisTemplate.opsForList().rightPush(key, msgJson);
        trimToWindowSize(key);
    }

    @Override
    public void clear(String conversationId) {
        String key = KEY_PREFIX + conversationId;
        redisTemplate.delete(key);
    }

    private void trimToWindowSize(String key) {
        Long size = redisTemplate.opsForList().size(key);
        if (size != null && size > slidingWindowSize) {
            redisTemplate.opsForList().trim(key, size - slidingWindowSize, -1);
        }
    }

    private String toJson(String role, String content) {
        return "{\"role\":\"" + role + "\",\"content\":\"" + escapeJson(content) + "\"}";
    }

    private Message parseMessage(String json) {
        try {
            int roleStart = json.indexOf("\"role\":\"") + 8;
            int roleEnd = json.indexOf("\"", roleStart);
            String role = json.substring(roleStart, roleEnd);

            int contentStart = json.indexOf("\"content\":\"") + 11;
            int contentEnd = json.lastIndexOf("\"}");
            String content = json.substring(contentStart, contentEnd);

            content = unescapeJson(content);

            return switch (role) {
                case "user" -> new UserMessage(content);
                case "assistant" -> new AssistantMessage(content);
                default -> new UserMessage(content);
            };
        } catch (Exception e) {
            log.error("Failed to parse message: {}", json, e);
            return new UserMessage(json);
        }
    }

    private String escapeJson(String text) {
        if (text == null) return "";
        return text.replace("\\", "\\\\")
                .replace("\"", "\\\"")
                .replace("\n", "\\n")
                .replace("\r", "\\r")
                .replace("\t", "\\t");
    }

    private String unescapeJson(String text) {
        if (text == null) return "";
        return text.replace("\\n", "\n")
                .replace("\\r", "\r")
                .replace("\\t", "\t")
                .replace("\\\"", "\"")
                .replace("\\\\", "\\");
    }
}
