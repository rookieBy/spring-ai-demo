package com.example.api.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

/**
 * Chat Request DTO
 */
@Data
public class ChatRequest {
    @NotBlank(message = "Message cannot be blank")
    private String message;

    private String model; // qwen, glm, deepseek

    private String conversationId;
}
