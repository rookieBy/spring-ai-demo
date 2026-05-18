# CLAUDE.md

This file provides guidance to Claude Code (claude.ai/code) when working with code in this repository.

## 项目概述

`newsay-server-ai` 是一个基于 Spring Boot 4.0 和 Spring AI 2.0 的多模块 Maven 项目，专注于 LLM（大语言模型）集成服务，支持多种模型提供商的流式对话功能。

## 模块架构

```
newsay-server-ai/
├── newsay-server-ai-common/     # 公共组件：异常处理、结果封装、工具类
├── newsay-server-ai-api/        # API 层：DTO 定义
├── newsay-server-ai-llm/        # LLM 核心模块：模型调用、聊天记忆、搜索增强
├── newsay-server-ai-business/   # 业务层：Controller 入口
└── newsay-server-ai-launcher/   # 启动模块：应用入口、打包配置
```

## 构建命令

```bash
# 编译指定模块（包含依赖）
mvn compile -s settings.xml -pl newsay-server-ai-llm -am

# 打包启动模块
mvn package -s settings.xml -DskipTests -pl newsay-server-ai-launcher -am

# 运行测试
mvn test -s settings.xml -pl newsay-server-ai-llm
```

> **注意**: 项目使用 `settings.xml` 指定本地 Maven 仓库路径（位于 Windows D 盘）。

## 核心技术栈

| 组件 | 版本 | 用途 |
|------|------|------|
| Spring Boot | 4.0.0 | 核心框架 |
| Spring AI | 2.0.0-M4 | LLM 集成 |
| Java | 21 | 运行环境 |
| MyBatis Plus | 3.0.5 | 数据访问 |
| Redisson | 3.27.0 | Redis 分布式锁/缓存 |
| Log4j2 | 2.23.1 | 日志 |

## LLM 架构

### 支持的模型提供商

- **DeepSeek**: `https://api.deepseek.com`
- **GLM (Zhipu)**: `https://open.bigmodel.cn/api/paas/v4`
- **Qwen (Alibaba)**: `https://dashscope.aliyuncs.com/compatible-mode/v1`
- **OpenAI**: `https://api.openai.com/v1`
- **MiniMax**: 支持 MCP（Model Context Protocol）搜索工具

### 核心服务

- `LlmService` - 统一接口，定义流式/非流式对话
- `LlmServiceImpl` - 实现类，负责模型路由、思考过程过滤
- `ChatMemoryService` - 对话历史管理（Redis 持久化）
- `SearchIntentAnalyzer` - 搜索意图识别（关键词匹配）
- `MinimaxMcpConfig` - MCP 搜索配置

### 流式端点

```
POST /api/chat/stream           # 基础流式对话
POST /api/chat/stream/markdown  # Markdown 格式化流式响应
POST /api/chat                  # 非流式对话
GET  /api/chat/search           # MCP 搜索
GET  /api/chat/models           # 获取支持的模型列表
```

### 搜索增强流程

1. 自动检测：关键词匹配判断是否需要搜索
2. MCP 优先：支持 MCP 的模型让 LLM 自行决定调用搜索
3. 降级策略：关键词匹配触发 MiniMax 搜索

## 关键配置

- `application.properties` - 主配置
- `application-{env}.properties` - 环境配置（dev/uat/pro）
- `log4j2-spring.xml` - 日志配置

## 代码规范

- **不可变性**: 优先使用 `record` 定义 DTO，使用 `final` 字段
- **依赖注入**: 构造函数注入（禁止字段注入）
- **异常处理**: 领域异常继承 `RuntimeException`，由 `GlobalExceptionHandler` 统一处理
- **日志**: 使用 Slf4j，避免直接使用 `System.out`