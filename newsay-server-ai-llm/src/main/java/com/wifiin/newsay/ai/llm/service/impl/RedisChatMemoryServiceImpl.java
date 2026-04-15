package com.wifiin.newsay.ai.llm.service.impl;

import com.wifiin.newsay.ai.llm.service.ChatMemoryService;
import com.wifiin.newsay.ai.llm.service.ChatMemoryPersistence;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.ai.chat.messages.AssistantMessage;
import org.springframework.ai.chat.messages.Message;
import org.springframework.ai.chat.messages.UserMessage;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * Redis-based Chat Memory Implementation with Sliding Window
 */
@Service
public class RedisChatMemoryServiceImpl implements ChatMemoryService {

    private static final Logger log = LoggerFactory.getLogger(RedisChatMemoryServiceImpl.class);
    private static final String KEY_PREFIX = "chat:memory:";

    private final StringRedisTemplate redisTemplate;
    private final int slidingWindowSize;
    private final int ttlSeconds;
    private ChatMemoryPersistence chatMemoryPersistence;

    public RedisChatMemoryServiceImpl(
            StringRedisTemplate redisTemplate,
            @Value("${spring.ai.chat.memory.sliding-window-size:20}") int slidingWindowSize,
            @Value("${spring.ai.chat.memory.ttl:3600}") int ttlSeconds) {
        this.redisTemplate = redisTemplate;
        this.slidingWindowSize = slidingWindowSize;
        this.ttlSeconds = ttlSeconds;
    }

    public void setChatMemoryPersistence(ChatMemoryPersistence persistence) {
        this.chatMemoryPersistence = persistence;
    }

    @Override
    public List<Message> getHistory(String conversationId) {
        String key = KEY_PREFIX + conversationId;
        List<String> messages = redisTemplate.opsForList().range(key, 0, -1);

        if (messages == null || messages.isEmpty()) {
            // Redis 未命中，尝试从 MySQL 加载
            if (chatMemoryPersistence != null) {
                Optional<List<Message>> dbMessages = chatMemoryPersistence.loadFromDatabase(conversationId);
                if (dbMessages.isPresent()) {
                    List<Message> loaded = dbMessages.get();
                    // 回填 Redis
                    for (Message msg : loaded) {
                        if (msg instanceof UserMessage) {
                            addUserMessage(conversationId, msg.getContent());
                        } else if (msg instanceof AssistantMessage) {
                            addAssistantMessage(conversationId, msg.getContent());
                        }
                    }
                    return loaded;
                }
            }
            return Collections.emptyList();
        }

        // 刷新 TTL
        redisTemplate.expire(key, Duration.ofSeconds(ttlSeconds));

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
        redisTemplate.expire(key, Duration.ofSeconds(ttlSeconds));  // 刷新 TTL
    }

    @Override
    public void addAssistantMessage(String conversationId, String assistantMessage) {
        String key = KEY_PREFIX + conversationId;
        String msgJson = toJson("assistant", assistantMessage);

        redisTemplate.opsForList().rightPush(key, msgJson);
        trimToWindowSize(key);
        redisTemplate.expire(key, Duration.ofSeconds(ttlSeconds));  // 刷新 TTL
    }

    @Override
    public void clear(String conversationId) {
        String key = KEY_PREFIX + conversationId;
        redisTemplate.delete(key);
    }

    public Set<String> getAllConversationIds() {
        Set<String> keys = redisTemplate.keys(KEY_PREFIX + "*");
        if (keys == null) return Collections.emptySet();
        return keys.stream()
                .map(k -> k.substring(KEY_PREFIX.length()))
                .collect(Collectors.toSet());
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
