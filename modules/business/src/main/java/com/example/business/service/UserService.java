package com.example.business.service;

import com.example.business.entity.User;
import java.util.List;

/**
 * User Service Interface
 */
public interface UserService {

    /**
     * Get user by ID
     */
    User getUserById(Long id);

    /**
     * Get all users
     */
    List<User> getAllUsers();

    /**
     * Search users by username or email
     */
    List<User> searchUsers(String keyword);

    /**
     * Create user
     */
    User createUser(User user);

    /**
     * Update user
     */
    User updateUser(User user);

    /**
     * Delete user
     */
    void deleteUser(Long id);
}
