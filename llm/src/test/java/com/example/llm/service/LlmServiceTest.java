package com.example.llm.service;

import com.example.llm.enums.LlmModel;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class LlmServiceTest {

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
}
