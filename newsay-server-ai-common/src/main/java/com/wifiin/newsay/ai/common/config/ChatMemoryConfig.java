package com.wifiin.newsay.ai.common.config;

import org.springframework.ai.chat.memory.ChatMemory;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.redis.core.StringRedisTemplate;

/**
 * ChatMemory Configuration using Redis with sliding window
 */
@Configuration
public class ChatMemoryConfig {

    /**
     * Default max messages per conversation (sliding window size)
     */
    private static final int DEFAULT_MAX_MESSAGES = 10;

    @Bean
    public ChatMemory chatMemory(StringRedisTemplate stringRedisTemplate) {
        RedisChatMemoryRepository repository = new RedisChatMemoryRepository(stringRedisTemplate);
        return new RedisChatMemory(repository, DEFAULT_MAX_MESSAGES);
    }
}
