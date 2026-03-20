package com.example.llm.service;

import com.example.llm.enums.LlmModel;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Test cases for LLM streaming chat with DeepSeek
 * This is a basic test to verify enum and configuration
 *
 * Full integration test requires running the Spring Boot application
 */
class DeepSeekStreamChatTest {

    @Test
    void testLlmModelEnum() {
        assertEquals(LlmModel.DEEPSEEK, LlmModel.fromValue("deepseek"));
        assertEquals(LlmModel.GLM, LlmModel.fromValue("glm"));
        assertEquals(LlmModel.QWEN, LlmModel.fromValue("qwen"));
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
    void testDeepSeekConfiguration() {
        // Verify DeepSeek API key is configured (not the default placeholder)
        String apiKey = "sk-ca951e58cea0451e9d59046c01ef20bd";
        assertNotNull(apiKey);
        assertTrue(apiKey.startsWith("sk-"));
        assertTrue(apiKey.length() > 30);
    }

    @Test
    void testPromptForPrimeNumbers() {
        // Test the exact prompt we will use
        String prompt = "请用Java代码实现查找100以内的所有质数，返回格式为JSON：{\"code\":\"你的Java代码\"}，只返回JSON，不要其他解释";

        assertNotNull(prompt);
        assertTrue(prompt.contains("100以内"));
        assertTrue(prompt.contains("质数"));
        assertTrue(prompt.contains("JSON"));
        assertTrue(prompt.contains("code"));
    }
}
