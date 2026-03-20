package com.example.business.service.impl;

import com.example.business.entity.User;
import com.example.business.mapper.UserMapper;
import com.example.business.service.UserService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.List;

/**
 * User Service Implementation
 */
@Service
public class UserServiceImpl implements UserService {

    private static final Logger log = LoggerFactory.getLogger(UserServiceImpl.class);

    @Autowired
    private UserMapper userMapper;

    @Override
    public User getUserById(Long id) {
        log.info("Getting user by ID: {}", id);
        return userMapper.selectById(id);
    }

    @Override
    public List<User> getAllUsers() {
        log.info("Getting all users");
        return userMapper.selectList(null);
    }

    @Override
    public List<User> searchUsers(String keyword) {
        log.info("Searching users with keyword: {}", keyword);
        return userMapper.searchUsers(keyword);
    }

    @Override
    public User createUser(User user) {
        log.info("Creating user: {}", user.getUsername());
        user.setCreateTime(LocalDateTime.now());
        userMapper.insert(user);
        return user;
    }

    @Override
    public User updateUser(User user) {
        log.info("Updating user: {}", user.getId());
        user.setUpdateTime(LocalDateTime.now());
        userMapper.updateById(user);
        return user;
    }

    @Override
    public void deleteUser(Long id) {
        log.info("Deleting user: {}", id);
        userMapper.deleteById(id);
    }
}
