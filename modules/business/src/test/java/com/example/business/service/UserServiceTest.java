package com.example.business.service;

import com.example.business.entity.User;
import com.example.llm.enums.LlmModel;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

/**
 * User Service Unit Tests
 * Note: Full integration tests require database setup
 */
class UserServiceTest {

    @Test
    void testLlmModelFromValue() {
        assertEquals(LlmModel.QWEN, LlmModel.fromValue("qwen"));
        assertEquals(LlmModel.GLM, LlmModel.fromValue("glm"));
        assertEquals(LlmModel.DEEPSEEK, LlmModel.fromValue("deepseek"));
        assertEquals(LlmModel.OPENAI, LlmModel.fromValue("openai"));
    }

    @Test
    void testLlmModelDefault() {
        assertEquals(LlmModel.QWEN, LlmModel.fromValue("unknown"));
        assertEquals(LlmModel.QWEN, LlmModel.fromValue(""));
        assertEquals(LlmModel.QWEN, LlmModel.fromValue(null));
    }

    @Test
    void testLlmModelValues() {
        assertEquals("qwen", LlmModel.QWEN.getValue());
        assertEquals("glm", LlmModel.GLM.getValue());
        assertEquals("deepseek", LlmModel.DEEPSEEK.getValue());
        assertEquals("openai", LlmModel.OPENAI.getValue());
    }

    @Test
    void testUserEntityCreation() {
        User user = new User();
        user.setId(1L);
        user.setUsername("testuser");
        user.setEmail("test@example.com");
        user.setStatus(1);

        assertEquals(1L, user.getId());
        assertEquals("testuser", user.getUsername());
        assertEquals("test@example.com", user.getEmail());
        assertEquals(1, user.getStatus());
    }
}
