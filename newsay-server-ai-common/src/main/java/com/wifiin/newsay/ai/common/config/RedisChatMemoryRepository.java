package com.wifiin.newsay.ai.common.config;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.ai.chat.messages.Message;
import org.springframework.ai.chat.memory.ChatMemoryRepository;
import org.springframework.data.redis.core.StringRedisTemplate;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;

/**
 * Redis implementation of ChatMemoryRepository
 * Stores conversation messages in Redis with automatic cleanup
 */
public class RedisChatMemoryRepository implements ChatMemoryRepository {

    private static final Logger log = LoggerFactory.getLogger(RedisChatMemoryRepository.class);

    private static final String KEY_PREFIX = "chat_memory:";
    private static final String CONVERSATION_IDS_KEY = "chat_memory:conversation_ids";

    private final StringRedisTemplate redisTemplate;
    private final ObjectMapper objectMapper;

    public RedisChatMemoryRepository(StringRedisTemplate redisTemplate) {
        this.redisTemplate = redisTemplate;
        this.objectMapper = new ObjectMapper();
        this.objectMapper.findAndRegisterModules();
    }

    private String getConversationKey(String conversationId) {
        return KEY_PREFIX + conversationId;
    }

    @Override
    public List<String> findConversationIds() {
        Set<String> keys = redisTemplate.keys(KEY_PREFIX + "*");
        List<String> ids = new ArrayList<>();
        if (keys != null) {
            for (String key : keys) {
                if (!key.equals(CONVERSATION_IDS_KEY)) {
                    ids.add(key.substring(KEY_PREFIX.length()));
                }
            }
        }
        return ids;
    }

    @Override
    public List<Message> findByConversationId(String conversationId) {
        String key = getConversationKey(conversationId);
        String json = redisTemplate.opsForValue().get(key);
        if (json == null || json.isEmpty()) {
            return new ArrayList<>();
        }
        try {
            return objectMapper.readValue(json, new TypeReference<List<Message>>() {});
        } catch (JsonProcessingException e) {
            log.error("Failed to deserialize messages for conversation: {}", conversationId, e);
            return new ArrayList<>();
        }
    }

    @Override
    public void saveAll(String conversationId, List<Message> messages) {
        if (messages == null || messages.isEmpty()) {
            return;
        }
        String key = getConversationKey(conversationId);
        try {
            String json = objectMapper.writeValueAsString(messages);
            redisTemplate.opsForValue().set(key, json);
            // Track this conversation ID
            redisTemplate.opsForSet().add(CONVERSATION_IDS_KEY, conversationId);
        } catch (JsonProcessingException e) {
            log.error("Failed to serialize messages for conversation: {}", conversationId, e);
        }
    }

    @Override
    public void deleteByConversationId(String conversationId) {
        String key = getConversationKey(conversationId);
        redisTemplate.delete(key);
        redisTemplate.opsForSet().remove(CONVERSATION_IDS_KEY, conversationId);
    }
}
