package com.example.launcher.controller;

import com.example.api.dto.UserQueryRequest;
import com.example.business.entity.User;
import com.example.business.service.UserService;
import com.example.common.result.Result;
import jakarta.validation.Valid;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * User Controller - Handles user-related business operations
 */
@Slf4j
@RestController
@RequestMapping("/api/users")
public class UserController {

    private final UserService userService;

    public UserController(UserService userService) {
        this.userService = userService;
    }

    /**
     * Get user by ID
     */
    @GetMapping("/{id}")
    public Result<User> getUserById(@PathVariable Long id) {
        log.info("Getting user by ID: {}", id);
        User user = userService.getUserById(id);
        return Result.success(user);
    }

    /**
     * Get all users
     */
    @GetMapping
    public Result<List<User>> getAllUsers() {
        log.info("Getting all users");
        List<User> users = userService.getAllUsers();
        return Result.success(users);
    }

    /**
     * Search users by keyword
     */
    @GetMapping("/search")
    public Result<List<User>> searchUsers(@RequestParam String keyword) {
        log.info("Searching users with keyword: {}", keyword);
        List<User> users = userService.searchUsers(keyword);
        return Result.success(users);
    }

    /**
     * Create new user
     */
    @PostMapping
    public Result<User> createUser(@Valid @RequestBody UserQueryRequest request) {
        log.info("Creating user: {}", request.getUsername());
        User user = new User();
        user.setUsername(request.getUsername());
        user.setEmail(request.getEmail());
        user.setStatus(1);
        User created = userService.createUser(user);
        return Result.success(created);
    }

    /**
     * Update user
     */
    @PutMapping("/{id}")
    public Result<User> updateUser(@PathVariable Long id, @RequestBody User user) {
        log.info("Updating user: {}", id);
        user.setId(id);
        User updated = userService.updateUser(user);
        return Result.success(updated);
    }

    /**
     * Delete user
     */
    @DeleteMapping("/{id}")
    public Result<Void> deleteUser(@PathVariable Long id) {
        log.info("Deleting user: {}", id);
        userService.deleteUser(id);
        return Result.success();
    }
}
