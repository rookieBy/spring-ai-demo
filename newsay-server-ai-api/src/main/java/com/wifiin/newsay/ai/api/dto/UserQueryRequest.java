package com.wifiin.newsay.ai.api.dto;

import jakarta.validation.constraints.NotNull;

/**
 * User Query Request DTO
 */
public class UserQueryRequest {
    @NotNull(message = "User ID cannot be null")
    private Long userId;

    private String username;

    private String email;

    public Long getUserId() {
        return userId;
    }

    public void setUserId(Long userId) {
        this.userId = userId;
    }

    public String getUsername() {
        return username;
    }

    public void setUsername(String username) {
        this.username = username;
    }

    public String getEmail() {
        return email;
    }

    public void setEmail(String email) {
        this.email = email;
    }
}
