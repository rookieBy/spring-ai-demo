# Chat Memory 两级缓存设计

## 背景

当前 Chat Memory 仅使用 Redis 存储对话历史，存在以下问题：
1. 对话永久存储，Redis 内存持续增长
2. 无降级能力，Redis 故障时对话历史丢失
3. 多轮对话测试发现上下文丢失问题，需要排查根因

## 目标

1. 实现 Redis + MySQL 两级缓存
2. Redis 对话过期时间 1 小时
3. 定时任务将活跃对话同步到 MySQL
4. 读取时 Redis 未命中则查 MySQL 并回填缓存
5. 排查多轮对话上下文丢失问题

## 架构设计

```
┌─────────────┐     ┌─────────────┐     ┌─────────────┐
│   Client    │────▶│  LLM API    │────▶│ ChatMemory  │
└─────────────┘     └─────────────┘     └─────────────┘
                                               │
                    ┌──────────────────────────┼──────────────────────────┐
                    │                          │                          │
                    ▼                          ▼                          ▼
            ┌───────────────┐         ┌───────────────┐         ┌───────────────┐
            │  Redis Cache  │         │  MySQL Store  │         │Scheduler Sync │
            │   (热数据)    │         │   (冷数据)    │         │  (定时任务)   │
            │   TTL: 1h     │         │   永久存储    │         │   每10分钟    │
            └───────────────┘         └───────────────┘         └───────────────┘
```

## 数据模型

### MySQL 表设计

```sql
CREATE TABLE chat_conversation (
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

### Redis Key 设计

- Key: `chat:memory:{conversationId}`
- Value: JSON 格式的消息列表
- TTL: 3600 秒（1小时）
- 刷新机制：每次读写操作时刷新 TTL

## 实现方案

### 1. 修改配置

**application-dev.properties**
```properties
# MySQL 本地开发配置
spring.datasource.mysql.jdbcUrl=jdbc:mysql://localhost:3306/newsay_chat?autoReconnect=true&useUnicode=true&characterEncoding=utf-8&cachePrepStmts=true&zeroDateTimeBehavior=convertToNull
spring.datasource.mysql.username=root
spring.datasource.mysql.password=root

# Redis TTL 配置（秒）
spring.ai.chat.memory.ttl=3600

# 同步任务配置
spring.ai.chat.memory.sync-interval-minutes=10
```

### 2. 新增 MySQL Repository

```java
public interface ChatConversationRepository {
    void upsert(String conversationId, List<Message> messages, LocalDateTime lastAccessTime);
    Optional<ChatConversation> findByConversationId(String conversationId);
    List<ChatConversation> findAllOrderByLastAccessTimeDesc(int limit);
}
```

### 3. 修改 RedisChatMemoryServiceImpl

- 增加 TTL 管理（每次读写刷新 TTL）
- 增加读写 MySQL 的回调接口

```java
public interface ChatMemoryPersistence {
    void saveToDatabase(String conversationId, List<Message> messages);
    Optional<List<Message>> loadFromDatabase(String conversationId);
}
```

### 4. 新增定时同步任务

```java
@Component
public class ChatMemorySyncTask {
    // 每10分钟执行
    // 1. 扫描 Redis 中所有 chat:memory:* keys
    // 2. 对每个对话，upsert 到 MySQL
    // 3. 如果对话超过TTL未访问则删除
}
```

### 5. 修改 ChatMemoryService 读取逻辑

```java
public List<Message> getHistory(String conversationId) {
    // 1. 先查 Redis
    List<Message> messages = redisTemplate.get(conversationId);
    if (messages != null) {
        return messages;
    }

    // 2. Redis 未命中，查 MySQL
    List<Message> dbMessages = chatConversationRepository.findByConversationId(conversationId);
    if (dbMessages.isPresent()) {
        // 3. 回填 Redis，TTL 1小时
        redisTemplate.set(conversationId, dbMessages.get());
    }
    return dbMessages.orElse(Collections.emptyList());
}
```

## 排查多轮对话问题

在实现过程中同时排查上下文丢失问题：

1. 验证 Redis 存储是否正确（添加日志）
2. 验证 getHistory 返回的数据是否正确
3. 验证 LLM 调用时 history 参数是否正确传递
4. 验证 MiniMax 模型是否正确处理多轮对话

## 关键文件修改

| 文件 | 操作 |
|------|------|
| `application-dev.properties` | 修改 MySQL 连接为 localhost:3306，新增库 newsay_chat |
| `chat_conversation` 表 | 新建 |
| `ChatConversation.java` | 新建实体类 |
| `ChatConversationRepository.java` | 新建 JPA Repository |
| `RedisChatMemoryServiceImpl.java` | 修改，增加 TTL 管理和 MySQL 回调 |
| `ChatMemoryService.java` | 新增接口方法 |
| `ChatMemoryPersistence.java` | 新建持久化接口 |
| `ChatMemorySyncTask.java` | 新建定时同步任务 |

## 风险与注意事项

1. 分布式环境下多实例部署时，定时任务需要分布式锁避免重复执行
2. 对话数据大时 JSON 可能较长，需要 MySQL JSON 类型支持
3. 首次部署需要手动创建数据库和表
