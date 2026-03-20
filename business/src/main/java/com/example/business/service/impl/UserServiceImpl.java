package com.example.business.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.example.business.entity.User;
import com.example.business.mapper.UserMapper;
import com.example.business.service.UserService;
import com.example.common.exception.BusinessException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.List;

/**
 * User Service Implementation
 */
@Slf4j
@Service
public class UserServiceImpl extends ServiceImpl<UserMapper, User> implements UserService {

    private final RedisTemplate<String, Object> redisTemplate;
    private static final String USER_CACHE_PREFIX = "user:";

    public UserServiceImpl(RedisTemplate<String, Object> redisTemplate) {
        this.redisTemplate = redisTemplate;
    }

    @Override
    public User getUserById(Long id) {
        String cacheKey = USER_CACHE_PREFIX + id;
        User cachedUser = (User) redisTemplate.opsForValue().get(cacheKey);
        if (cachedUser != null) {
            log.info("User found in cache: {}", id);
            return cachedUser;
        }

        User user = getById(id);
        if (user != null) {
            redisTemplate.opsForValue().set(cacheKey, user);
            log.info("User cached: {}", id);
        }
        return user;
    }

    @Override
    public List<User> getAllUsers() {
        return list();
    }

    @Override
    public List<User> searchUsers(String keyword) {
        LambdaQueryWrapper<User> wrapper = new LambdaQueryWrapper<>();
        wrapper.like(User::getUsername, keyword)
                .or()
                .like(User::getEmail, keyword);
        return list(wrapper);
    }

    @Override
    public User createUser(User user) {
        user.setCreateTime(LocalDateTime.now());
        user.setUpdateTime(LocalDateTime.now());
        if (!save(user)) {
            throw new BusinessException("Failed to create user");
        }
        log.info("User created: {}", user.getId());
        return user;
    }

    @Override
    public User updateUser(User user) {
        user.setUpdateTime(LocalDateTime.now());
        if (!updateById(user)) {
            throw new BusinessException("Failed to update user");
        }

        // Invalidate cache
        redisTemplate.delete(USER_CACHE_PREFIX + user.getId());
        log.info("User updated and cache invalidated: {}", user.getId());
        return user;
    }

    @Override
    public void deleteUser(Long id) {
        if (!removeById(id)) {
            throw new BusinessException("Failed to delete user");
        }
        redisTemplate.delete(USER_CACHE_PREFIX + id);
        log.info("User deleted and cache invalidated: {}", id);
    }
}
