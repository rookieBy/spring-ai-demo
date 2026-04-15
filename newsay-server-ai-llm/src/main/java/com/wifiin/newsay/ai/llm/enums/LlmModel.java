package com.wifiin.newsay.ai.llm.enums;

/**
 * LLM Model Types
 */
public enum LlmModel {
    QWEN("qwen"),
    GLM("glm"),
    DEEPSEEK("deepseek"),
    OPENAI("openai"),
    MINIMAX("minimax");

    private final String value;

    LlmModel(String value) {
        this.value = value;
    }

    public String getValue() {
        return value;
    }

    public static LlmModel fromValue(String value) {
        for (LlmModel model : values()) {
            if (model.value.equalsIgnoreCase(value)) {
                return model;
            }
        }
        return QWEN; // default
    }
}
