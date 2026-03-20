package com.example.launcher.controller;

import com.example.api.dto.ChatRequest;
import com.example.llm.enums.LlmModel;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

/**
 * LLM Controller Unit Tests
 * Note: Full integration tests require Spring context
 */
class LlmControllerTest {

    private final ObjectMapper objectMapper = new ObjectMapper();

    @Test
    void testChatRequestSerialization() throws Exception {
        ChatRequest request = new ChatRequest();
        request.setMessage("Hello");
        request.setModel("qwen");

        String json = objectMapper.writeValueAsString(request);
        assertTrue(json.contains("Hello"));
        assertTrue(json.contains("qwen"));
    }

    @Test
    void testLlmModelEnum() {
        assertEquals(LlmModel.QWEN, LlmModel.fromValue("qwen"));
        assertEquals(LlmModel.GLM, LlmModel.fromValue("glm"));
        assertEquals(LlmModel.DEEPSEEK, LlmModel.fromValue("deepseek"));
        assertEquals(LlmModel.OPENAI, LlmModel.fromValue("openai"));
    }
}
