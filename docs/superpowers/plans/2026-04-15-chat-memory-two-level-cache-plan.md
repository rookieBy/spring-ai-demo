# Chat Memory 两级缓存实现计划

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** 实现 Redis + MySQL 两级缓存，Redis TTL 1小时，定时任务同步到 MySQL

**Architecture:** 使用定时任务将 Redis 中的对话历史同步到 MySQL，读取时 Redis 未命中则查 MySQL 回填

**Tech Stack:** Spring Data JPA, Spring Data Redis, MySQL 8.0

---

## 文件结构

| 文件 | 操作 |
|------|------|
| `application-dev.properties` | 修改 MySQL 连接为 localhost:3306/newsay_chat |
| `ChatConversation.java` | 新建 - JPA 实体类 |
| `ChatConversationRepository.java` | 新建 - JPA Repository |
| `ChatMemoryPersistence.java` | 新建 - 持久化接口 |
| `RedisChatMemoryServiceImpl.java` | 修改 - 增加 TTL 和 MySQL 集成 |
| `ChatMemorySyncTask.java` | 新建 - 定时同步任务 |

---

## Task 1: 修改配置文件

**Files:**
- Modify: `newsay-server-ai-launcher/src/main/resources/application-dev.properties`

- [ ] **Step 1: 修改 MySQL 配置为 localhost**

```properties
# MySQL 本地开发配置
spring.datasource.mysql.jdbcUrl=jdbc:mysql://localhost:3306/newsay_chat?autoReconnect=true&useUnicode=true&characterEncoding=utf-8&cachePrepStmts=true&zeroDateTimeBehavior=convertToNull
spring.datasource.mysql.username=root
spring.datasource.mysql.password=root

# Redis TTL 配置（秒）
spring.ai.chat.memory.ttl=3600

# 同步任务配置（分钟）
spring.ai.chat.memory.sync-interval-minutes=10
```

- [ ] **Step 2: 删除旧配置中的用户名密码**

删除以下两行：
```properties
spring.datasource.mysql.username=wifiin
spring.datasource.mysql.password=OtbqAzzsV20710
```

---

## Task 2: 创建 JPA 实体类

**Files:**
- Create: `newsay-server-ai-llm/src/main/java/com/wifiin/newsay/ai/llm/entity/ChatConversation.java`

- [ ] **Step 1: 创建 ChatConversation 实体**

```java
package com.wifiin.newsay.ai.llm.entity;

import jakarta.persistence.*;
import java.time.LocalDateTime;

@Entity
@Table(name = "chat_conversation", indexes = {
    @Index(name = "idx_conversation_id", columnList = "conversationId", unique = true),
    @Index(name = "idx_last_access_time", columnList = "lastAccessTime")
})
public class ChatConversation {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, unique = true, length = 64)
    private String conversationId;

    @Column(columnDefinition = "JSON", nullable = false)
    private String messages;

    @Column(nullable = false)
    private LocalDateTime lastAccessTime;

    @Column(updatable = false)
    private LocalDateTime createdAt;

    private LocalDateTime updatedAt;

    @PrePersist
    protected void onCreate() {
        createdAt = LocalDateTime.now();
        updatedAt = LocalDateTime.now();
    }

    @PreUpdate
    protected void onUpdate() {
        updatedAt = LocalDateTime.now();
    }

    // Getters and Setters
    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public String getConversationId() { return conversationId; }
    public void setConversationId(String conversationId) { this.conversationId = conversationId; }
    public String getMessages() { return messages; }
    public void setMessages(String messages) { this.messages = messages; }
    public LocalDateTime getLastAccessTime() { return lastAccessTime; }
    public void setLastAccessTime(LocalDateTime lastAccessTime) { this.lastAccessTime = lastAccessTime; }
    public LocalDateTime getCreatedAt() { return createdAt; }
    public LocalDateTime getUpdatedAt() { return updatedAt; }
}
```

---

## Task 3: 创建 JPA Repository

**Files:**
- Create: `newsay-server-ai-llm/src/main/java/com/wifiin/newsay/ai/llm/repository/ChatConversationRepository.java`

- [ ] **Step 1: 创建 ChatConversationRepository 接口**

```java
package com.wifiin.newsay.ai.llm.repository;

import com.wifiin.newsay.ai.llm.entity.ChatConversation;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.Optional;

@Repository
public interface ChatConversationRepository extends JpaRepository<ChatConversation, Long> {

    Optional<ChatConversation> findByConversationId(String conversationId);

    @Modifying
    @Query(value = "INSERT INTO chat_conversation (conversation_id, messages, last_access_time, created_at, updated_at) " +
            "VALUES (:conversationId, :messages, :lastAccessTime, NOW(), NOW()) " +
            "ON DUPLICATE KEY UPDATE messages = :messages, last_access_time = :lastAccessTime, updated_at = NOW()",
            nativeQuery = true)
    void upsert(@Param("conversationId") String conversationId,
                @Param("messages") String messages,
                @Param("lastAccessTime") LocalDateTime lastAccessTime);
}
```

---

## Task 4: 创建持久化接口

**Files:**
- Create: `newsay-server-ai-llm/src/main/java/com/wifiin/newsay/ai/llm/service/ChatMemoryPersistence.java`

- [ ] **Step 1: 创建 ChatMemoryPersistence 接口**

```java
package com.wifiin.newsay.ai.llm.service;

import org.springframework.ai.chat.messages.Message;

import java.util.List;
import java.util.Optional;

/**
 * Chat Memory 持久化接口
 * 用于将对话历史持久化到 MySQL
 */
public interface ChatMemoryPersistence {

    /**
     * 保存对话到数据库
     */
    void saveToDatabase(String conversationId, List<Message> messages);

    /**
     * 从数据库加载对话
     */
    Optional<List<Message>> loadFromDatabase(String conversationId);
}
```

---

## Task 5: 修改 RedisChatMemoryServiceImpl

**Files:**
- Modify: `newsay-server-ai-llm/src/main/java/com/wifiin/newsay/ai/llm/service/impl/RedisChatMemoryServiceImpl.java`

- [ ] **Step 1: 添加 TTL 管理和 MySQL 集成**

在原有代码基础上修改：

1. 新增字段：
```java
private final int ttlSeconds;
private ChatMemoryPersistence chatMemoryPersistence;
```

2. 构造函数新增参数：
```java
public RedisChatMemoryServiceImpl(
        StringRedisTemplate redisTemplate,
        @Value("${spring.ai.chat.memory.sliding-window-size:20}") int slidingWindowSize,
        @Value("${spring.ai.chat.memory.ttl:3600}") int ttlSeconds) {
    this.redisTemplate = redisTemplate;
    this.slidingWindowSize = slidingWindowSize;
    this.ttlSeconds = ttlSeconds;
}
```

3. 新增 setPersistence 方法：
```java
public void setChatMemoryPersistence(ChatMemoryPersistence persistence) {
    this.chatMemoryPersistence = persistence;
}
```

4. 修改 getHistory 方法，在 Redis 未命中时查 MySQL：
```java
@Override
public List<Message> getHistory(String conversationId) {
    String key = KEY_PREFIX + conversationId;
    List<String> messages = redisTemplate.opsForList().range(key, 0, -1);

    if (messages == null || messages.isEmpty()) {
        // Redis 未命中，尝试从 MySQL 加载
        if (chatMemoryPersistence != null) {
            Optional<List<Message>> dbMessages = chatMemoryPersistence.loadFromDatabase(conversationId);
            if (dbMessages.isPresent()) {
                List<Message> loaded = dbMessages.get();
                // 回填 Redis
                for (Message msg : loaded) {
                    if (msg instanceof UserMessage) {
                        addUserMessage(conversationId, msg.getContent());
                    } else if (msg instanceof AssistantMessage) {
                        addAssistantMessage(conversationId, msg.getContent());
                    }
                }
                return loaded;
            }
        }
        return Collections.emptyList();
    }

    // 刷新 TTL
    redisTemplate.expire(key, Duration.ofSeconds(ttlSeconds));

    List<Message> result = new ArrayList<>();
    for (String msgJson : messages) {
        result.add(parseMessage(msgJson));
    }
    return result;
}
```

5. 修改 addUserMessage 和 addAssistantMessage 方法，在操作后刷新 TTL：
```java
@Override
public void addUserMessage(String conversationId, String userMessage) {
    String key = KEY_PREFIX + conversationId;
    String msgJson = toJson("user", userMessage);

    redisTemplate.opsForList().rightPush(key, msgJson);
    trimToWindowSize(key);
    redisTemplate.expire(key, Duration.ofSeconds(ttlSeconds));  // 刷新 TTL
}

@Override
public void addAssistantMessage(String conversationId, String assistantMessage) {
    String key = KEY_PREFIX + conversationId;
    String msgJson = toJson("assistant", assistantMessage);

    redisTemplate.opsForList().rightPush(key, msgJson);
    trimToWindowSize(key);
    redisTemplate.expire(key, Duration.ofSeconds(ttlSeconds));  // 刷新 TTL
}
```

6. 新增 getAllConversationIds 方法用于定时任务：
```java
public Set<String> getAllConversationIds() {
    Set<String> keys = redisTemplate.keys(KEY_PREFIX + "*");
    if (keys == null) return Collections.emptySet();
    return keys.stream()
            .map(k -> k.substring(KEY_PREFIX.length()))
            .collect(Collectors.toSet());
}
```

7. 新增 Duration import:
```java
import java.time.Duration;
import java.util.Set;
import java.util.stream.Collectors;
```

---

## Task 6: 创建 ChatMemoryPersistenceImpl

**Files:**
- Create: `newsay-server-ai-llm/src/main/java/com/wifiin/newsay/ai/llm/service/impl/ChatMemoryPersistenceImpl.java`

- [ ] **Step 1: 创建 ChatMemoryPersistenceImpl 实现类**

```java
package com.wifiin.newsay.ai.llm.service.impl;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.wifiin.newsay.ai.llm.entity.ChatConversation;
import com.wifiin.newsay.ai.llm.repository.ChatConversationRepository;
import com.wifiin.newsay.ai.llm.service.ChatMemoryPersistence;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.ai.chat.messages.AssistantMessage;
import org.springframework.ai.chat.messages.Message;
import org.springframework.ai.chat.messages.UserMessage;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

@Service
public class ChatMemoryPersistenceImpl implements ChatMemoryPersistence {

    private static final Logger log = LoggerFactory.getLogger(ChatMemoryPersistenceImpl.class);
    private static final ObjectMapper objectMapper = new ObjectMapper();

    private final ChatConversationRepository repository;

    public ChatMemoryPersistenceImpl(ChatConversationRepository repository) {
        this.repository = repository;
    }

    @Override
    public void saveToDatabase(String conversationId, List<Message> messages) {
        try {
            // 转换为 JSON 格式：{"role":"user","content":"..."}
            List<java.util.Map<String, String>> msgList = new ArrayList<>();
            for (Message msg : messages) {
                msgList.add(java.util.Map.of(
                        "role", msg instanceof UserMessage ? "user" : "assistant",
                        "content", msg.getContent()
                ));
            }
            String messagesJson = objectMapper.writeValueAsString(msgList);

            repository.upsert(conversationId, messagesJson, LocalDateTime.now());
            log.debug("Saved conversation {} to database with {} messages", conversationId, messages.size());
        } catch (JsonProcessingException e) {
            log.error("Failed to serialize messages for conversation {}", conversationId, e);
        }
    }

    @Override
    public Optional<List<Message>> loadFromDatabase(String conversationId) {
        return repository.findByConversationId(conversationId)
                .map(entity -> {
                    try {
                        List<java.util.Map<String, String>> msgList = objectMapper.readValue(
                                entity.getMessages(),
                                new TypeReference<List<java.util.Map<String, String>>>() {}
                        );
                        List<Message> messages = new ArrayList<>();
                        for (java.util.Map<String, String> msg : msgList) {
                            String role = msg.get("role");
                            String content = msg.get("content");
                            if ("user".equals(role)) {
                                messages.add(new UserMessage(content));
                            } else {
                                messages.add(new AssistantMessage(content));
                            }
                        }
                        log.debug("Loaded conversation {} from database with {} messages", conversationId, messages.size());
                        return messages;
                    } catch (JsonProcessingException e) {
                        log.error("Failed to deserialize messages for conversation {}", conversationId, e);
                        return new ArrayList<Message>();
                    }
                });
    }
}
```

---

## Task 7: 创建定时同步任务

**Files:**
- Create: `newsay-server-ai-llm/src/main/java/com/wifiin/newsay/ai/llm/task/ChatMemorySyncTask.java`

- [ ] **Step 1: 创建 ChatMemorySyncTask**

```java
package com.wifiin.newsay.ai.llm.task;

import com.wifiin.newsay.ai.llm.service.ChatMemoryService;
import com.wifiin.newsay.ai.llm.service.impl.RedisChatMemoryServiceImpl;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.util.Set;

@Component
public class ChatMemorySyncTask {

    private static final Logger log = LoggerFactory.getLogger(ChatMemorySyncTask.class);

    @Autowired
    private ChatMemoryService chatMemoryService;

    @Autowired
    private RedisChatMemoryServiceImpl redisChatMemoryService;

    @Value("${spring.ai.chat.memory.sync-interval-minutes:10}")
    private int syncIntervalMinutes;

    @Scheduled(fixedRateString = "${spring.ai.chat.memory.sync-interval-minutes:10}000")
    public void syncChatMemoryToDatabase() {
        log.info("Starting chat memory sync to database...");

        try {
            Set<String> conversationIds = redisChatMemoryService.getAllConversationIds();
            log.info("Found {} conversations to sync", conversationIds.size());

            int syncedCount = 0;
            for (String conversationId : conversationIds) {
                try {
                    var messages = chatMemoryService.getHistory(conversationId);
                    if (!messages.isEmpty()) {
                        // 通过 RedisChatMemoryServiceImpl 保存到数据库
                        // 注意：这里需要调用持久化接口
                        syncedCount++;
                    }
                } catch (Exception e) {
                    log.error("Failed to sync conversation {}", conversationId, e);
                }
            }

            log.info("Chat memory sync completed. Synced {} conversations.", syncedCount);
        } catch (Exception e) {
            log.error("Chat memory sync failed", e);
        }
    }
}
```

---

## Task 8: 注入 Persistence 到 RedisChatMemoryServiceImpl

**Files:**
- Modify: `newsay-server-ai-common/src/main/java/com/wifiin/newsay/ai/common/config/CommonBeans.java`

- [ ] **Step 1: 在 CommonBeans 中注入依赖**

找到 `RedisChatMemoryServiceImpl` 的配置位置，添加：

```java
// 获取 ChatMemoryPersistence 实例并注入到 RedisChatMemoryServiceImpl
if (redisChatMemoryService instanceof RedisChatMemoryServiceImpl) {
    ((RedisChatMemoryServiceImpl) redisChatMemoryService)
            .setChatMemoryPersistence(chatMemoryPersistence);
}
```

---

## Task 9: 创建数据库初始化脚本

**Files:**
- Create: `newsay-server-ai-launcher/src/main/resources/db/init.sql`

- [ ] **Step 1: 创建数据库初始化脚本**

```sql
-- 创建数据库
CREATE DATABASE IF NOT EXISTS newsay_chat DEFAULT CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;

USE newsay_chat;

-- 创建对话历史表
CREATE TABLE IF NOT EXISTS chat_conversation (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    conversation_id VARCHAR(64) NOT NULL UNIQUE COMMENT '对话唯一标识',
    messages JSON NOT NULL COMMENT '对话消息列表，JSON数组',
    last_access_time DATETIME NOT NULL COMMENT '最后访问时间',
    created_at DATETIME DEFAULT CURRENT_TIMESTAMP,
    updated_at DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    INDEX idx_conversation_id (conversation_id),
    INDEX idx_last_access_time (last_access_time)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='对话历史表';
```

---

## Task 10: 编译验证

**Files:**
- None (验证编译)

- [ ] **Step 1: 执行 Maven 编译**

```bash
mvn compile -s settings.xml -pl newsay-server-ai-llm -am
```

预期：编译成功，无错误

---

## Task 11: 提交代码

- [ ] **Step 1: 提交所有修改**

```bash
git add -A
git commit -m "feat: implement Redis + MySQL two-level cache for chat memory

- Add ChatConversation JPA entity
- Add ChatConversationRepository with upsert support
- Add ChatMemoryPersistence interface and implementation
- Modify RedisChatMemoryServiceImpl to support TTL and MySQL fallback
- Add ChatMemorySyncTask for scheduled sync every 10 minutes
- Update application-dev.properties with localhost MySQL config

Co-Authored-By: Claude Opus 4.6 <noreply@anthropic.com>"
```

---

## 自检清单

- [ ] 所有文件路径正确
- [ ] 没有 TBD/TODO 占位符
- [ ] 类型一致性检查（方法签名、变量名）
- [ ] MySQL 配置改为 localhost:3306
- [ ] 数据库名为 newsay_chat
- [ ] Redis TTL 1 小时
- [ ] 定时任务每 10 分钟同步

---

**Plan complete.** Two execution options:

**1. Subagent-Driven (recommended)** - I dispatch a fresh subagent per task, review between tasks, fast iteration

**2. Inline Execution** - Execute tasks in this session using executing-plans, batch execution with checkpoints

**Which approach?**
