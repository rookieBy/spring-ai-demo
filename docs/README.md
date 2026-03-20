# Spring AI Demo Project

Spring AI 多模块示例项目，集成多种大语言模型（Qwen、GLM、DeepSeek）并提供流式输出，支持业务数据操作。

## 项目结构

```
spring-ai-demo/
├── pom.xml                          # 根 POM 文件
├── modules/
│   ├── common/                      # 公共模块
│   │   ├── pom.xml
│   │   └── src/main/java/com/example/common/
│   │       ├── config/              # 配置类（Redis等）
│   │       ├── exception/           # 异常处理
│   │       ├── result/              # 统一响应
│   │       └── utils/               # 工具类
│   │
│   ├── api/                          # API模块
│   │   ├── pom.xml
│   │   └── src/main/java/com/example/api/
│   │           └── dto/             # 数据传输对象
│   │
│   ├── llm/                          # LLM集成模块
│   │   ├── pom.xml
│   │   └── src/main/java/com/example/llm/
│   │           ├── enums/           # 模型枚举
│   │           └── service/         # LLM服务
│   │
│   ├── business/                     # 业务模块
│   │   ├── pom.xml
│   │   └── src/main/java/com/example/business/
│   │           ├── entity/          # 实体类
│   │           ├── mapper/          # MyBatis Mapper
│   │           └── service/         # 业务服务
│   │
│   └── launcher/                     # 启动模块（聚合模块）
│       ├── pom.xml
│       └── src/
│           ├── main/
│           │   ├── java/com/example/launcher/
│           │   │   ├── Application.java      # 启动类
│           │   │   └── controller/            # 控制器
│           │   └── resources/
│           │       ├── application.yml        # 应用配置
│           │       └── schema.sql             # 数据库脚本
│           └── test/                          # 测试类
```

## 模块说明

### 1. common 模块
公共基础模块，提供通用组件：
- **Result**: 统一API响应格式
- **BusinessException**: 业务异常
- **GlobalExceptionHandler**: 全局异常处理
- **RedisConfig**: Redis配置

### 2. api 模块
API定义模块，包含请求/响应DTO：
- **ChatRequest**: 聊天请求
- **UserQueryRequest**: 用户查询请求

### 3. llm 模块
大语言模型集成模块：
- 支持模型：Qwen、GLM、DeepSeek、OpenAI
- **LlmService**: LLM服务接口
- **LlmServiceImpl**: 实现类，提供流式和非流式输出

### 4. business 模块
业务逻辑模块：
- **User**: 用户实体
- **UserMapper**: MyBatis Plus Mapper
- **UserService**: 用户服务（CRUD + 缓存）

### 5. launcher 模块
启动聚合模块：
- 聚合所有子模块
- 提供RESTful API接口
- 配置数据源和Redis

## 技术栈

| 技术 | 版本 | 说明 |
|------|------|------|
| JDK | 17 | Java开发版本 |
| Spring Boot | 3.2.3 | 基础框架 |
| Spring AI | 1.0.0-M6 | AI集成 |
| MyBatis Plus | 3.5.5 | ORM框架 |
| Druid | 1.2.21 | 数据库连接池 |
| Redis | - | 缓存 |
| Redisson | 3.27.0 | Redis客户端 |
| Lombok | 1.18.30 | 简化代码 |

## 依赖配置

### MySQL
```yaml
spring:
  datasource:
    driver-class-name: com.mysql.cj.jdbc.Driver
    url: jdbc:mysql://localhost:3306/spring_ai_demo
    username: root
    password: root
```

### Redis
```yaml
spring:
  data:
    redis:
      host: localhost
      port: 6379
```

### Spring AI (OpenAI兼容)
```yaml
spring.ai:
  openai:
    api-key: your-api-key
    base-url: https://api.openai.com/v1
```

## API接口

### LLM接口

#### 1. 流式聊天
```http
POST /api/llm/chat/stream
Content-Type: application/json

{
    "message": "Hello, how are you?",
    "model": "qwen"
}
```

响应类型：`text/event-stream`

#### 2. 普通聊天
```http
POST /api/llm/chat
Content-Type: application/json

{
    "message": "Hello, how are you?",
    "model": "glm"
}
```

响应：
```json
{
    "code": 200,
    "message": "success",
    "data": "I'm doing well, thank you!",
    "timestamp": 1710000000000
}
```

#### 3. 获取支持的模型
```http
GET /api/llm/models
```

响应：
```json
{
    "code": 200,
    "data": {
        "qwen": "Alibaba Qwen",
        "glm": "Zhipu GLM",
        "deepseek": "DeepSeek",
        "openai": "OpenAI GPT"
    }
}
```

### 用户接口

#### 1. 获取用户
```http
GET /api/users/{id}
```

#### 2. 获取所有用户
```http
GET /api/users
```

#### 3. 搜索用户
```http
GET /api/users/search?keyword=admin
```

#### 4. 创建用户
```http
POST /api/users
Content-Type: application/json

{
    "username": "newuser",
    "email": "newuser@example.com"
}
```

#### 5. 更新用户
```http
PUT /api/users/{id}
Content-Type: application/json

{
    "username": "updated",
    "email": "updated@example.com"
}
```

#### 6. 删除用户
```http
DELETE /api/users/{id}
```

## 如何运行

### 1. 环境准备

- JDK 17+
- Maven 3.8+
- MySQL 8.0+
- Redis 6.0+

### 2. 创建数据库

```sql
CREATE DATABASE spring_ai_demo DEFAULT CHARACTER SET utf8mb4;
```

### 3. 配置API密钥

在 `application.yml` 或环境变量中配置：

```yaml
spring:
  ai:
    openai:
      api-key: your-actual-api-key
```

或设置环境变量：
```bash
export OPENAI_API_KEY=your-actual-api-key
```

### 4. 构建项目

```bash
cd spring-ai-demo
mvn clean install -DskipTests
```

### 5. 运行应用

```bash
cd modules/launcher
mvn spring-boot:run
```

或打包后运行：
```bash
mvn clean package -DskipTests
java -jar modules/launcher/target/launcher-1.0.0.jar
```

### 6. 验证运行

访问健康检查：
```http
GET http://localhost:8080/actuator/health
```

## 测试

### 运行单元测试

```bash
mvn test
```

### 测试特定模块

```bash
# 测试LLM模块
mvn test -pl modules/llm

# 测试业务模块
mvn test -pl modules/business
```

## 支持的LLM模型

| 模型 | 说明 | 配置 |
|------|------|------|
| qwen | 阿里通义千问 | 通过OpenAI兼容接口 |
| glm | 智谱GLM | 通过OpenAI兼容接口 |
| deepseek | 深度求索 | 通过OpenAI兼容接口 |
| openai | OpenAI GPT | 原生OpenAI接口 |

## 特性说明

### 1. 流式输出
使用 Server-Sent Events (SSE) 实现流式输出，实时返回LLM响应。

### 2. 多模型支持
统一的接口支持多种LLM，通过模型参数切换。

### 3. Redis缓存
用户数据使用Redis缓存，减少数据库压力。

### 4. 统一响应
所有API使用统一的响应格式，便于前端处理。

## 注意事项

1. 首次运行需要配置有效的API密钥
2. MySQL和Redis服务需要先启动
3. 数据库表会自动创建（如果不存在）
4. 建议使用 `application-dev.yml` 分离开发环境配置

## License

MIT License
