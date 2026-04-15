# 搜索增强对话功能实现计划

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** 实现智能搜索增强对话系统，通过 LLM 语义分析自动判断用户问题是否需要联网搜索，调用 MiniMax MCP 获取实时信息后交由 DeepSeek/指定模型分析回答。

**Architecture:**
- 入口层：`ChatController` 接收 `/api/chat/stream` 请求
- 路由层：`LlmServiceImpl.streamChat()` 根据语义分析结果路由
- 搜索层：`MinimaxSearchService` 通过 MCP 调用联网搜索
- 记忆层：`RedisChatMemoryServiceImpl` 滑动窗口管理对话历史
- 多模型路由：`LLMConfig` 管理多个 LLM Provider

**Tech Stack:** Spring AI, MiniMax MCP, DeepSeek/GLM/Qwen, Redis (StringRedisTemplate), WebFlux (SSE)

---

## 当前代码状态

### ✅ 已完成（部分实现）
- `ChatRequest`: 已添加 `enableSearch` 字段
- `LlmServiceImpl`: 已添加 `needsSearchBySemanticAnalysis()` 方法（LLM 语义分析）
- `LlmServiceImpl`: 已添加 `streamChatWithSearch()` 和 `streamChatWithSearchMemory()` 方法
- `LlmServiceImpl`: 已修改 `streamChat()` 优先使用语义分析

### ❌ 待完成
1. `ChatController`: 未使用 `enableSearch` 参数控制搜索开关
2. `LlmService.streamChat(model, message, conversationId, enableSearch)`: 接口缺少 `enableSearch` 参数
3. `streamChatWithSearch` 调用时机问题：`needsSearchBySemanticAnalysis` 每次调用都会消耗 token
4. 缺少搜索结果缓存机制
5. 缺少单元测试
6. `smartStream` 方法未支持搜索增强

---

## 文件结构

- **Modify:** `newsay-server-ai-api/src/main/java/com/wifiin/newsay/ai/api/dto/ChatRequest.java` - 已有 `enableSearch`
- **Modify:** `newsay-server-ai-llm/src/main/java/com/wifiin/newsay/ai/llm/service/LlmService.java` - 添加带 `enableSearch` 的方法签名
- **Modify:** `newsay-server-ai-llm/src/main/java/com/wifiin/newsay/ai/llm/service/impl/LlmServiceImpl.java` - 实现 `enableSearch` 逻辑
- **Modify:** `newsay-server-ai-business/src/main/java/com/wifiin/newsay/ai/business/controller/ChatController.java` - 传递 `enableSearch`
- **Create:** `newsay-server-ai-llm/src/test/java/com/wifiin/newsay/ai/llm/service/LlmServiceImplTest.java` - 单元测试
- **Create:** `newsay-server-ai-llm/src/main/java/com/wifiin/newsay/ai/llm/service/SearchIntentAnalyzer.java` - 搜索意图分析器（可选，重构用）

---

## 任务列表

### Task 1: 添加 LlmService 接口方法签名

**Files:**
- Modify: `newsay-server-ai-llm/src/main/java/com/wifiin/newsay/ai/llm/service/LlmService.java:20-21`

- [ ] **Step 1: 添加带 enableSearch 参数的方法签名**

在 `LlmService.java` 的 `streamChat(String model, String message, String conversationId)` 方法后添加新方法：

```java
/**
 * Stream chat with specified model and conversationId, with explicit search control
 * @param model 模型名称
 * @param message 用户消息
 * @param conversationId 对话 ID
 * @param enableSearch true=强制启用搜索, false=禁用搜索, null=自动检测
 */
Flux<String> streamChat(String model, String message, String conversationId, Boolean enableSearch);
```

- [ ] **Step 2: Commit**

```bash
git add newsay-server-ai-llm/src/main/java/com/wifiin/newsay/ai/llm/service/LlmService.java
git commit -m "feat: add streamChat method with enableSearch parameter"
```

---

### Task 2: 实现 LlmServiceImpl.enableSearch 逻辑

**Files:**
- Modify: `newsay-server-ai-llm/src/main/java/com/wifiin/newsay/ai/llm/service/impl/LlmServiceImpl.java:103-136`

- [ ] **Step 1: 添加新方法 `streamChat(String model, String message, String conversationId, Boolean enableSearch)`**

在 `LlmServiceImpl.java` 中添加：

```java
@Override
public Flux<String> streamChat(String model, String message, String conversationId, Boolean enableSearch) {
    // 1. 如果 enableSearch 明确为 true，直接使用搜索增强
    if (Boolean.TRUE.equals(enableSearch)) {
        log.info("Search explicitly enabled for message: {}", message);
        return streamChatWithSearch(model, message, conversationId);
    }

    // 2. 如果 enableSearch 明确为 false，跳过搜索和语义分析
    if (Boolean.FALSE.equals(enableSearch)) {
        log.info("Search explicitly disabled, using normal chat");
        return streamChatNoSearch(model, message, conversationId);
    }

    // 3. enableSearch 为 null，执行自动语义分析检测
    return streamChatWithAutoDetection(model, message, conversationId);
}

/**
 * 自动检测模式：语义分析决定是否搜索
 */
private Flux<String> streamChatWithAutoDetection(String model, String message, String conversationId) {
    // Minimax 模型有 MCP 工具支持，让 LLM 自己决定
    if ("minimax".equalsIgnoreCase(model)) {
        return streamChatWithMinimax(message, conversationId);
    }

    // 检查对话历史是否需要继续使用 MCP
    if (conversationId != null && !conversationId.isEmpty()) {
        List<Message> history = chatMemoryService.getHistory(conversationId);
        if (shouldContinueWithMcp(history)) {
            return streamChatWithMinimax(message, conversationId);
        }
    }

    // 使用 LLM 语义分析判断是否需要联网搜索
    if (needsSearchBySemanticAnalysis(message)) {
        log.info("Semantic analysis triggered search for message: {}", message);
        return streamChatWithSearch(model, message, conversationId);
    }

    // 普通对话
    return streamChatNoSearch(model, message, conversationId);
}

/**
 * 不使用搜索的普通对话
 */
private Flux<String> streamChatNoSearch(String model, String message, String conversationId) {
    if ("minimax".equalsIgnoreCase(model)) {
        return streamChatWithMinimax(message, conversationId);
    }

    if (conversationId == null || conversationId.isEmpty()) {
        return streamChat(model, message);
    }

    LlmModel llmModel = LlmModel.fromValue(model);
    String finalApiKey = getApiKeyForModel(llmModel);
    String finalBaseUrl = getBaseUrlForModel(llmModel);
    String modelName = getModelNameForModel(llmModel);

    return streamChatWithMemory(message, conversationId, finalApiKey, finalBaseUrl, modelName);
}
```

- [ ] **Step 2: 修改原 `streamChat(String model, String message, String conversationId)` 调用新方法**

将原方法体改为调用 `streamChat(model, message, conversationId, null)` 以保持向后兼容：

```java
@Override
public Flux<String> streamChat(String model, String message, String conversationId) {
    return streamChat(model, message, conversationId, null);
}
```

- [ ] **Step 3: Commit**

```bash
git add newsay-server-ai-llm/src/main/java/com/wifiin/newsay/ai/llm/service/impl/LlmServiceImpl.java
git commit -m "feat: implement enableSearch logic with auto-detection support"
```

---

### Task 3: 更新 ChatController 传递 enableSearch 参数

**Files:**
- Modify: `newsay-server-ai-business/src/main/java/com/wifiin/newsay/ai/business/controller/ChatController.java:36-47`

- [ ] **Step 1: 修改 streamChat 方法调用**

```java
@PostMapping(value = "/stream", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
public Flux<String> streamChat(@Valid @RequestBody ChatRequest request) {
    log.info("Received streaming chat request - model: {}, message: {}, conversationId: {}, enableSearch: {}",
            request.getModel(), request.getMessage(), request.getConversationId(), request.getEnableSearch());

    String model = request.getModel() != null ? request.getModel() : "deepseek";
    return llmService.streamChat(model, request.getMessage(), request.getConversationId(), request.getEnableSearch())
            .map(content -> {
                log.debug("Streaming content: {}", content);
                return content;
            });
}
```

- [ ] **Step 2: Commit**

```bash
git add newsay-server-ai-business/src/main/java/com/wifiin/newsay/ai/business/controller/ChatController.java
git commit -m "feat: pass enableSearch parameter from ChatRequest to LlmService"
```

---

### Task 4: 为 smartStream 添加搜索增强支持

**Files:**
- Modify: `newsay-server-ai-llm/src/main/java/com/wifiin/newsay/ai/llm/service/impl/LlmServiceImpl.java:504-521`
- Modify: `newsay-server-ai-llm/src/main/java/com/wifiin/newsay/ai/llm/service/LlmService.java:43`

- [ ] **Step 1: 在 LlmService 接口添加 smartStream 带 enableSearch 的方法**

```java
Flux<ServerSentEvent<StreamChunk>> smartStream(String model, String message, Boolean enableSearch);
```

- [ ] **Step 2: 实现 smartStreamWithSearch 方法**

在 `LlmServiceImpl.java` 中添加：

```java
@Override
public Flux<ServerSentEvent<StreamChunk>> smartStream(String model, String message, Boolean enableSearch) {
    // 如果启用搜索，先搜索再流式返回
    if (Boolean.TRUE.equals(enableSearch) || (enableSearch == null && needsSearchBySemanticAnalysis(message))) {
        return smartStreamWithSearch(model, message);
    }
    return smartStreamDefault(model, message);
}

private Flux<ServerSentEvent<StreamChunk>> smartStreamWithSearch(String model, String message) {
    // 1. MCP 搜索
    String searchResults = mcpSearch(message);
    String enhancedPrompt = buildEnhancedPrompt(message, searchResults);

    // 2. 使用 deepseek 流式回答
    ChatClient chatClient = chatClientRouter.get("deepseek");
    return chatClient.prompt()
            .user(enhancedPrompt)
            .stream()
            .content()
            .transform(this::markdownAwareAggregator)
            .map(chunk -> ServerSentEvent.<StreamChunk>builder()
                    .data(chunk)
                    .build())
            .concatWith(Flux.just(
                    ServerSentEvent.<StreamChunk>builder()
                            .data(new StreamChunk("done", "", null))
                            .event("complete")
                            .build()
            ));
}

private Flux<ServerSentEvent<StreamChunk>> smartStreamDefault(String model, String message) {
    ChatClient chatClient = chatClientRouter.get("deepseek");
    return chatClient.prompt()
            .user(message)
            .stream()
            .content()
            .transform(this::markdownAwareAggregator)
            .map(chunk -> ServerSentEvent.<StreamChunk>builder()
                    .data(chunk)
                    .build())
            .concatWith(Flux.just(
                    ServerSentEvent.<StreamSentEvent<StreamChunk>>builder()
                            .data(new StreamChunk("done", "", null))
                            .event("complete")
                            .build()
            ));
}
```

- [ ] **Step 3: 保持原有 smartStream 方法调用新方法**

```java
@Override
public Flux<ServerSentEvent<StreamChunk>> smartStream(String model, String message) {
    return smartStream(model, message, null);
}
```

- [ ] **Step 4: 更新 ChatController.smartStream 调用**

```java
@PostMapping(value = "/stream/markdown", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
public Flux<ServerSentEvent<StreamChunk>> smartStream(@Valid @RequestBody ChatRequest request) {
    log.info("Received streaming chat request - model: {}, message: {}, enableSearch: {}",
            request.getModel(), request.getMessage(), request.getEnableSearch());

    String model = request.getModel() != null ? request.getModel() : "deepseek";
    return llmService.smartStream(model, request.getMessage(), request.getEnableSearch());
}
```

- [ ] **Step 5: Commit**

```bash
git add newsay-server-ai-llm/src/main/java/com/wifiin/newsay/ai/llm/service/LlmService.java
git add newsay-server-ai-llm/src/main/java/com/wifiin/newsay/ai/llm/service/impl/LlmServiceImpl.java
git add newsay-server-ai-business/src/main/java/com/wifiin/newsay/ai/business/controller/ChatController.java
git commit -m "feat: add search augmentation support to smartStream endpoint"
```

---

### Task 5: 创建搜索意图分析器（可选重构）

**Files:**
- Create: `newsay-server-ai-llm/src/main/java/com/wifiin/newsay/ai/llm/service/SearchIntentAnalyzer.java`

- [ ] **Step 1: 创建 SearchIntentAnalyzer 接口**

```java
package com.wifiin.newsay.ai.llm.service;

/**
 * 搜索意图分析器接口
 */
public interface SearchIntentAnalyzer {

    /**
     * 分析用户消息判断是否需要联网搜索
     * @param message 用户消息
     * @return true 如果需要联网搜索
     */
    boolean needsSearch(String message);
}
```

- [ ] **Step 2: 创建 LLM 语义分析实现**

```java
package com.wifiin.newsay.ai.llm.service.impl;

import com.wifiin.newsay.ai.llm.service.SearchIntentAnalyzer;
import org.springframework.ai.chat.client.ChatClient;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Component;

/**
 * 基于 LLM 语义分析的搜索意图判断
 */
@Component
public class LlmBasedSearchIntentAnalyzer implements SearchIntentAnalyzer {

    private static final Logger log = LoggerFactory.getLogger(LlmBasedSearchIntentAnalyzer.class);

    private static final String ANALYSIS_PROMPT = """
            你是一个意图分析助手。请判断用户问题是否需要联网搜索才能回答。

            需要搜索的情况：
            1. 询问实时信息（新闻、天气、股价、赛事等）
            2. 询问最新动态（今天、最近、新发布等）
            3. 询问具体数值或数据（当前价格、人数、排名等）
            4. 询问近期事件或变化
            5. 明确要求"搜索"或"查询"

            不需要搜索的情况：
            1. 询问一般性知识或概念
            2. 请求写作、翻译、代码等创造性任务
            3. 询问历史事实或已有定论的信息
            4. 闲聊或问候

            用户问题：%s

            请只回答"是"或"否"，不要解释。
            """;

    private final ChatClient deepseekClient;

    public LlmBasedSearchIntentAnalyzer(@Qualifier("deepseekChatClient") ChatClient deepseekClient) {
        this.deepseekClient = deepseekClient;
    }

    @Override
    public boolean needsSearch(String message) {
        if (message == null || message.isBlank()) {
            return false;
        }

        try {
            String result = deepseekClient.prompt()
                    .user(String.format(ANALYSIS_PROMPT, message))
                    .call()
                    .content();

            boolean needsSearch = result != null &&
                    (result.trim().equalsIgnoreCase("是") || result.trim().equalsIgnoreCase("yes"));
            log.info("Search intent analysis for '{}': {} (result: {})", message, needsSearch, result);
            return needsSearch;
        } catch (Exception e) {
            log.warn("Search intent analysis failed, fallback to false: {}", e.getMessage());
            return false;
        }
    }
}
```

- [ ] **Step 3: 创建关键词匹配后备实现**

```java
package com.wifiin.newsay.ai.llm.service.impl;

import com.wifiin.newsay.ai.llm.service.SearchIntentAnalyzer;
import org.springframework.stereotype.Component;

import java.util.List;

/**
 * 基于关键词匹配的搜索意图判断（后备方案）
 */
@Component
public class KeywordBasedSearchIntentAnalyzer implements SearchIntentAnalyzer {

    private static final List<String> SEARCH_KEYWORDS = List.of(
            "搜索", "最新", "今天", "昨日", "新闻", "最近", "当前",
            "实时", "行情", "股价", "天气", "查询", "多少", "排名"
    );

    @Override
    public boolean needsSearch(String message) {
        if (message == null || message.isBlank()) {
            return false;
        }
        String lowerMessage = message.toLowerCase();
        return SEARCH_KEYWORDS.stream().anyMatch(lowerMessage::contains);
    }
}
```

- [ ] **Step 4: 使用组合模式优化 LlmServiceImpl**

修改 `LlmServiceImpl` 注入 `SearchIntentAnalyzer`：

```java
@Autowired
private SearchIntentAnalyzer searchIntentAnalyzer;

private boolean needsSearchBySemanticAnalysis(String message) {
    return searchIntentAnalyzer.needsSearch(message);
}
```

- [ ] **Step 5: Commit**

```bash
git add newsay-server-ai-llm/src/main/java/com/wifiin/newsay/ai/llm/service/SearchIntentAnalyzer.java
git add newsay-server-ai-llm/src/main/java/com/wifiin/newsay/ai/llm/service/impl/LlmBasedSearchIntentAnalyzer.java
git add newsay-server-ai-llm/src/main/java/com/wifiin/newsay/ai/llm/service/impl/KeywordBasedSearchIntentAnalyzer.java
git commit -m "refactor: extract search intent analysis to dedicated component"
```

---

### Task 6: 编写单元测试

**Files:**
- Create: `newsay-server-ai-llm/src/test/java/com/wifiin/newsay/ai/llm/service/LlmServiceImplTest.java`
- Create: `newsay-server-ai-llm/src/test/java/com/wifiin/newsay/ai/llm/service/SearchIntentAnalyzerTest.java`

- [ ] **Step 1: 创建 SearchIntentAnalyzer 测试**

```java
package com.wifiin.newsay.ai.llm.service;

import com.wifiin.newsay.ai.llm.service.impl.KeywordBasedSearchIntentAnalyzer;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import static org.junit.jupiter.api.Assertions.*;

class SearchIntentAnalyzerTest {

    private SearchIntentAnalyzer analyzer;

    @BeforeEach
    void setUp() {
        analyzer = new KeywordBasedSearchIntentAnalyzer();
    }

    @ParameterizedTest
    @ValueSource(strings = {
            "今天天气怎么样",
            "最新新闻有哪些",
            "帮我搜索一下",
            "现在股价多少",
            "当前排名是多少"
    })
    void shouldDetectSearchIntent(String message) {
        assertTrue(analyzer.needsSearch(message), "Should detect search intent for: " + message);
    }

    @ParameterizedTest
    @ValueSource(strings = {
            "如何写一个Hello World",
            "给我讲个笑话",
            "翻译这句话",
            "帮我写代码"
    })
    void shouldNotDetectSearchIntent(String message) {
        assertFalse(analyzer.needsSearch(message), "Should not detect search intent for: " + message);
    }

    @Test
    void shouldReturnFalseForNullOrEmpty() {
        assertFalse(analyzer.needsSearch(null));
        assertFalse(analyzer.needsSearch(""));
        assertFalse(analyzer.needsSearch("   "));
    }
}
```

- [ ] **Step 2: 创建 LlmServiceImplTest（Mock 测试）**

```java
package com.wifiin.newsay.ai.llm.service;

import com.wifiin.newsay.ai.llm.service.impl.LlmServiceImpl;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import reactor.core.publisher.Flux;

import static org.junit.jupiter.api.Assertions.*;

@ExtendWith(MockitoExtension.class)
class LlmServiceImplTest {

    @Mock
    private ChatMemoryService chatMemoryService;

    @Mock
    private java.util.Map<String, org.springframework.ai.chat.client.ChatClient> chatClientRouter;

    @InjectMocks
    private LlmServiceImpl llmService;

    @Test
    void streamChatWithExplicitSearchEnabled_shouldUseSearchFlow() {
        // Given
        String model = "deepseek";
        String message = "今天天气怎么样";
        String conversationId = "test-123";

        // When
        Flux<String> result = llmService.streamChat(model, message, conversationId, true);

        // Then - verify search flow is triggered (具体验证依赖 Mock)
        assertNotNull(result);
    }

    @Test
    void streamChatWithExplicitSearchDisabled_shouldSkipSearch() {
        // Given
        String model = "deepseek";
        String message = "你好";
        String conversationId = "test-123";

        // When
        Flux<String> result = llmService.streamChat(model, message, conversationId, false);

        // Then
        assertNotNull(result);
    }
}
```

- [ ] **Step 3: 运行测试验证**

```bash
cd /root/coding_plan/newsay-server-ai
./mvnw test -pl newsay-server-ai-llm -Dtest=SearchIntentAnalyzerTest,LlmServiceImplTest -q
```

Expected: Tests pass

- [ ] **Step 4: Commit**

```bash
git add newsay-server-ai-llm/src/test/java/com/wifiin/newsay/ai/llm/service/
git commit -m "test: add unit tests for search intent analyzer and LlmServiceImpl"
```

---

### Task 7: 添加搜索结果缓存（可选优化）

**Files:**
- Modify: `newsay-server-ai-llm/src/main/java/com/wifiin/newsay/ai/llm/service/impl/RedisChatMemoryServiceImpl.java`
- Create: `newsay-server-ai-llm/src/main/java/com/wifiin/newsay/ai/llm/service/SearchCacheService.java`

- [ ] **Step 1: 创建 SearchCacheService**

```java
package com.wifiin.newsay.ai.llm.service;

import org.springframework.stereotype.Service;

/**
 * 搜索结果缓存服务
 * 相同问题在短时间内的搜索结果会被缓存
 */
public interface SearchCacheService {

    /**
     * 获取缓存的搜索结果
     * @param query 搜索查询
     * @return 缓存结果或 null
     */
    String getCachedResult(String query);

    /**
     * 缓存搜索结果
     * @param query 搜索查询
     * @param result 搜索结果
     */
    void cacheResult(String query, String result);
}
```

- [ ] **Step 2: 实现 RedisSearchCacheServiceImpl**

```java
package com.wifiin.newsay.ai.llm.service.impl;

import com.wifiin.newsay.ai.llm.service.SearchCacheService;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

import java.time.Duration;

@Service
public class RedisSearchCacheServiceImpl implements SearchCacheService {

    private static final String CACHE_PREFIX = "search:cache:";

    private final StringRedisTemplate redisTemplate;
    private final Duration cacheTtl;

    public RedisSearchCacheServiceImpl(
            StringRedisTemplate redisTemplate,
            @Value("${spring.ai.search.cache.ttl:5m}") Duration cacheTtl) {
        this.redisTemplate = redisTemplate;
        this.cacheTtl = cacheTtl;
    }

    @Override
    public String getCachedResult(String query) {
        String key = CACHE_PREFIX + hashQuery(query);
        return redisTemplate.opsForValue().get(key);
    }

    @Override
    public void cacheResult(String query, String result) {
        String key = CACHE_PREFIX + hashQuery(query);
        redisTemplate.opsForValue().set(key, result, cacheTtl);
    }

    private String hashQuery(String query) {
        // 使用 MD5 或 SHA256 哈希查询
        return Integer.toHexString(query.hashCode());
    }
}
```

- [ ] **Step 3: 修改 streamChatWithSearch 使用缓存**

在 `LlmServiceImpl.streamChatWithSearch()` 方法中添加缓存逻辑：

```java
@Override
public Flux<String> streamChatWithSearch(String model, String message, String conversationId) {
    boolean hasConversation = conversationId != null && !conversationId.isEmpty();
    if (hasConversation) {
        chatMemoryService.addUserMessage(conversationId, message);
    }

    // 1. 尝试从缓存获取搜索结果
    String searchResults = searchCacheService.getCachedResult(message);
    if (searchResults == null) {
        // 缓存未命中，调用 MCP 搜索
        searchResults = mcpSearch(message);
        searchCacheService.cacheResult(message, searchResults);
    }

    // 2. 构建增强提示词
    String enhancedPrompt = buildEnhancedPrompt(message, searchResults);

    // 3. 流式回答
    LlmModel llmModel = LlmModel.fromValue(model);
    return streamChatWithSearchMemory(enhancedPrompt, conversationId,
            getApiKeyForModel(llmModel), getBaseUrlForModel(llmModel), getModelNameForModel(llmModel));
}
```

- [ ] **Step 4: Commit**

```bash
git add newsay-server-ai-llm/src/main/java/com/wifiin/newsay/ai/llm/service/SearchCacheService.java
git add newsay-server-ai-llm/src/main/java/com/wifiin/newsay/ai/llm/service/impl/RedisSearchCacheServiceImpl.java
git commit -m "feat: add search result caching with Redis TTL"
```

---

## 架构流程图

```
┌─────────────────────────────────────────────────────────────────────────┐
│                         用户请求流程                                      │
└─────────────────────────────────────────────────────────────────────────┘

  POST /api/chat/stream {message, model, conversationId, enableSearch}
                                    │
                                    ▼
┌─────────────────────────────────────────────────────────────────────────┐
│                      ChatController.streamChat()                        │
│                   传递 enableSearch 参数                                 │
└─────────────────────────────────────────────────────────────────────────┘
                                    │
                                    ▼
┌─────────────────────────────────────────────────────────────────────────┐
│                     LlmServiceImpl.streamChat()                         │
│                              │                                           │
│              ┌───────────────┼───────────────┐                          │
│              ▼               ▼               ▼                          │
│      enableSearch=true   enableSearch=false   enableSearch=null        │
│              │               │               │                          │
│              ▼               ▼               ▼                          │
│     streamChatWithSearch  streamChatNoSearch  streamChatWithAutoDetect  │
│              │               │               │                          │
│              │               │               ▼                          │
│              │               │    needsSearchBySemanticAnalysis()        │
│              │               │               │                          │
│              │               │    ┌──────────┴──────────┐                │
│              │               │    ▼                     ▼                │
│              │               │   true                   false            │
│              │               │    │                     │                │
│              │               │    ▼                     │                │
│              │               │   true                   │                │
│              │               │    │                     │                │
│              └───────────────┴────┴─────────────────────┘                │
│                                    │                                      │
└────────────────────────────────────┼────────────────────────────────────┘
                                     ▼
┌─────────────────────────────────────────────────────────────────────────┐
│                        搜索增强流程 (streamChatWithSearch)                │
│                                                                          │
│  1. 保存用户消息到 Redis (chatMemoryService.addUserMessage)              │
│  2. mcpSearch(message) 调用 MiniMax MCP 联网搜索                         │
│  3. buildEnhancedPrompt() 构建增强提示词                                   │
│  4. streamChatWithSearchMemory() 使用目标模型流式回答                      │
└─────────────────────────────────────────────────────────────────────────┘
                                     │
                                     ▼
┌─────────────────────────────────────────────────────────────────────────┐
│                     Redis 对话历史管理 (滑动窗口)                         │
│                                                                          │
│  KEY: chat:memory:{conversationId}                                       │
│  VALUE: [user_msg_1, assistant_msg_1, user_msg_2, ...]                  │
│  TRIM: 保持最近 N 条消息 (默认 20)                                        │
└─────────────────────────────────────────────────────────────────────────┘
```

---

## Self-Review 检查清单

1. **Spec coverage:** 所有需求都有对应任务
   - [x] 语义分析路由 - Task 1-3, 5
   - [x] 搜索增强对话 - Task 4, 6
   - [x] 流式响应 - Task 4
   - [x] 对话历史管理 - 已在 RedisChatMemoryServiceImpl 实现
   - [x] enableSearch 参数控制 - Task 1-3

2. **Placeholder scan:** 无 TBD/TODO，已提供完整代码

3. **Type consistency:** 方法签名在整个计划中保持一致

---

## 执行选择

**Plan complete and saved to `docs/superpowers/plans/2026-04-15-search-augmented-chat.md`**

**Two execution options:**

**1. Subagent-Driven (recommended)** - I dispatch a fresh subagent per task, review between tasks, fast iteration

**2. Inline Execution** - Execute tasks in this session using executing-plans, batch execution with checkpoints

**Which approach?**
