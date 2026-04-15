package com.wifiin.newsay.ai.api.dto;

import jakarta.validation.constraints.NotBlank;

/**
 * Chat Request DTO
 */
public class ChatRequest {
    @NotBlank(message = "Message cannot be blank")
    private String message;

    private String model; // qwen, glm, deepseek

    private String conversationId;

    /**
     * 是否启用联网搜索增强（可选）
     * true: 强制启用搜索
     * false: 禁用搜索
     * null: 自动检测（默认）
     */
    private Boolean enableSearch;

    public String getMessage() {
        return message;
    }

    public void setMessage(String message) {
        this.message = message;
    }

    public String getModel() {
        return model;
    }

    public void setModel(String model) {
        this.model = model;
    }

    public String getConversationId() {
        return conversationId;
    }

    public void setConversationId(String conversationId) {
        this.conversationId = conversationId;
    }

    public Boolean getEnableSearch() {
        return enableSearch;
    }

    public void setEnableSearch(Boolean enableSearch) {
        this.enableSearch = enableSearch;
    }
}
