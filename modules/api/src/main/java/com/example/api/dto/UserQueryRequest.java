package com.example.api.dto;

import jakarta.validation.constraints.NotNull;
import lombok.Data;

/**
 * User Query Request DTO
 */
@Data
public class UserQueryRequest {
    @NotNull(message = "User ID cannot be null")
    private Long userId;

    private String username;

    private String email;
}
