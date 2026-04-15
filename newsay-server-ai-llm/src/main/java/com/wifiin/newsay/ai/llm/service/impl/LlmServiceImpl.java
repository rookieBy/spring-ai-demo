package com.wifiin.newsay.ai.llm.service.impl;

import com.wifiin.newsay.ai.llm.enums.LlmModel;
import com.wifiin.newsay.ai.llm.model.StreamChunk;
import com.wifiin.newsay.ai.llm.service.ChatMemoryService;
import com.wifiin.newsay.ai.llm.service.LLMProperties;
import com.wifiin.newsay.ai.llm.service.LlmService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.messages.AssistantMessage;
import org.springframework.ai.chat.messages.Message;
import org.springframework.ai.chat.messages.UserMessage;
import org.springframework.ai.chat.model.ChatResponse;
import org.springframework.ai.chat.prompt.Prompt;
import org.springframework.ai.openai.OpenAiChatModel;
import org.springframework.ai.openai.OpenAiChatOptions;
import org.springframework.ai.openai.api.OpenAiApi;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.codec.ServerSentEvent;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Flux;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicReference;
import java.util.regex.Pattern;

/**
 * LLM Service Implementation supporting multiple LLM providers via OpenAI-compatible API
 * Supports: DeepSeek, GLM (Zhipu), Qwen (Alibaba), OpenAI
 * <p>
 * All models use OpenAI-compatible endpoints with different base URLs:
 * - DeepSeek: https://api.deepseek.com
 * - GLM: https://open.bigmodel.cn/api/paas/v4
 * - Qwen: https://dashscope.aliyuncs.com/compatible-mode/v1
 * - OpenAI: https://api.openai.com/v1
 */
@Service
public class LlmServiceImpl implements LlmService {

    private static final Logger log = LoggerFactory.getLogger(LlmServiceImpl.class);

    @Value("${spring.ai.openai.api-key}")
    private String openAiApiKey;

    @Value("${spring.ai.openai.base-url}")
    private String openAiBaseUrl;

    @Value("${spring.ai.deepseek.api-key}")
    private String deepSeekApiKey;

    @Value("${spring.ai.deepseek.base-url:https://api.deepseek.com}")
    private String deepSeekBaseUrl;

    @Value("${spring.ai.alibaba.api-key:}")
    private String glmApiKey;

    @Value("${spring.ai.alibaba.base-url:https://open.bigmodel.cn/api/paas/v4}")
    private String glmBaseUrl;

    @Value("${spring.ai.dashscope.api-key:}")
    private String dashScopeApiKey;

    @Value("${spring.ai.dashscope.base-url:https://dashscope.aliyuncs.com/compatible-mode/v1}")
    private String dashScopeBaseUrl;


    // Markdown 格式标记
    private static final Pattern CODE_FENCE = Pattern.compile("```(\\w*)?");
    private static final Pattern LIST_ITEM = Pattern.compile("^[\\s]*[-*+\\d.]\\s");
    private static final Pattern TABLE_ROW = Pattern.compile("^\\|.+\\|$");
    private static final Pattern HEADER = Pattern.compile("^#{1,6}\\s");


    @Autowired
    @Qualifier("llmRouter")
    private Map<String, ChatClient> chatClientRouter;

    @Autowired
    private ChatMemoryService chatMemoryService;

    @Autowired
    private LLMProperties llmProperties;


    @Override
    public Flux<String> streamChat(String model, String message) {
        LlmModel llmModel = LlmModel.fromValue(model);

        String finalApiKey = getApiKeyForModel(llmModel);
        String finalBaseUrl = getBaseUrlForModel(llmModel);
        String modelName = getModelNameForModel(llmModel);


        return streamChat(message, finalApiKey, finalBaseUrl, modelName);
    }

    @Override
    public Flux<String> streamChat(String message) {
        return streamChat("deepseek", message, null);
    }

    @Override
    public Flux<String> streamChat(String model, String message, String conversationId) {
        return streamChat(model, message, conversationId, (Boolean) null);
    }

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
     * 自动检测模式：根据模型是否支持MCP来决定路由
     *
     * - 支持MCP的模型：让LLM自己决定是否调用搜索工具
     * - 不支持MCP的模型：用关键词匹配判断是否需要搜索，如果需要则调用MiniMax搜索
     */
    private Flux<String> streamChatWithAutoDetection(String model, String message, String conversationId) {
        // 检查当前模型是否支持MCP
        LLMProperties.ProviderConfig config = llmProperties.getProvider(model);
        boolean supportMcp = config != null && config.isMcp();

        if (supportMcp) {
            // 模型支持MCP，让LLM自己决定是否调用搜索工具
            return streamChatWithMinimax(message, conversationId);
        }

        // 模型不支持MCP，用关键词匹配判断是否需要搜索
        if (isSearchQuery(message)) {
            log.info("Keyword detected search need for message: {}", message);
            return streamChatWithSearch(model, message, conversationId);
        }

        // 普通对话
        return streamChatNoSearch(model, message, conversationId);
    }

    /**
     * 不使用搜索的普通对话
     */
    private Flux<String> streamChatNoSearch(String model, String message, String conversationId) {
        if (LlmModel.MINIMAX.getValue().equalsIgnoreCase(model)) {
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

    /**
     * 使用 Minimax MCP 流式对话（LLM自己决定是否调用搜索工具）
     */
    private Flux<String> streamChatWithMinimax(String message, String conversationId) {
        ChatClient chatClient = chatClientRouter.get(LlmModel.MINIMAX.getValue());

        final List<Message> history;
        final boolean hasConversation = conversationId != null && !conversationId.isEmpty();

        if (hasConversation) {
            history = chatMemoryService.getHistory(conversationId);
            chatMemoryService.addUserMessage(conversationId, message);
        } else {
            history = null;
        }

        StringBuilder fullResponse = new StringBuilder();

        return Flux.create(sink -> {
            var promptBuilder = chatClient.prompt();
            if (history != null && !history.isEmpty()) {
                promptBuilder.messages(history.toArray(new Message[0]));
            }
            promptBuilder.user(message);

            promptBuilder.stream()
                    .content()
                    .subscribe(
                            response -> {
                                fullResponse.append(response);
                                sink.next(response);
                            },
                            sink::error,
                            () -> {
                                if (hasConversation) {
                                    chatMemoryService.addAssistantMessage(conversationId, fullResponse.toString());
                                }
                                sink.complete();
                            }
                    );
        });
    }

    private Flux<String> streamChat(String message, String apiKey, String baseUrl, String model) {
        OpenAiApi tempApi = OpenAiApi.builder()
                .apiKey(apiKey)
                .baseUrl(baseUrl)
                .build();

        OpenAiChatModel tempChatModel = OpenAiChatModel.builder()
                .openAiApi(tempApi)
                .defaultOptions(OpenAiChatOptions.builder()
                        .model(model)
                        .temperature(0.7)
                        .build())
                .build();

        Prompt prompt = new Prompt(new UserMessage(message));

        return Flux.create(sink -> {
            tempChatModel.stream(prompt)
                    .subscribe(
                            response -> {
                                String content = extractContent(response);
                                if (content != null && !content.isEmpty()) {
                                    sink.next(content);
                                }
                            },
                            sink::error,
                            sink::complete
                    );
        });
    }

    private Flux<String> streamChatWithMemory(String message, String conversationId, String apiKey, String baseUrl, String model) {
        OpenAiApi tempApi = OpenAiApi.builder()
                .apiKey(apiKey)
                .baseUrl(baseUrl)
                .build();

        OpenAiChatModel tempChatModel = OpenAiChatModel.builder()
                .openAiApi(tempApi)
                .defaultOptions(OpenAiChatOptions.builder()
                        .model(model)
                        .temperature(0.7)
                        .build())
                .build();

        ChatClient tempChatClient = ChatClient.builder(tempChatModel).build();

        List<Message> history = chatMemoryService.getHistory(conversationId);

        StringBuilder fullResponse = new StringBuilder();

        chatMemoryService.addUserMessage(conversationId, message);

        return Flux.create(sink -> {
            tempChatClient.prompt()
                    .messages(history.toArray(new Message[0]))
                    .user(message)
                    .stream()
                    .content()
                    .subscribe(
                            response -> {
                                fullResponse.append(response);
                                sink.next(response);
                            },
                            sink::error,
                            () -> {
                                chatMemoryService.addAssistantMessage(conversationId, fullResponse.toString());
                                sink.complete();
                            }
                    );
        });
    }

    @Override
    public Flux<String> streamChatWithSearch(String model, String message, String conversationId) {
        // 1. 先用 Minimax MCP 搜索获取实时信息
        // 注意：mcpSearch 是同步阻塞调用，这是当前设计的权衡。
        // 搜索结果作为增强上下文整合到响应中，用户会先等待搜索完成，再开始接收流式响应。
        // 如需完全非阻塞，可考虑将搜索结果缓存或异步预取。
        String searchResults = mcpSearch(message);

        // 2. 构建增强提示词：搜索结果 + 用户问题
        String enhancedPrompt = buildEnhancedPrompt(message, searchResults);

        // 3. 用用户选择的模型流式回答
        LlmModel llmModel = LlmModel.fromValue(model);
        String finalApiKey = getApiKeyForModel(llmModel);
        String finalBaseUrl = getBaseUrlForModel(llmModel);
        String modelName = getModelNameForModel(llmModel);

        return streamChatWithMemory(enhancedPrompt, conversationId, finalApiKey, finalBaseUrl, modelName);
    }

    private String buildEnhancedPrompt(String question, String searchResults) {
        return "你是一个helpful的AI助手。以下是联网搜索获取的最新信息：\n\n" +
               "【搜索结果】\n" + searchResults + "\n\n" +
               "【用户问题】" + question + "\n\n" +
               "请根据以上搜索结果，回答用户的问题。如果搜索结果不相关，请基于你的知识回答。";
    }

    private boolean isSearchQuery(String message) {
        if (message == null || message.isEmpty()) {
            return false;
        }
        String lowerMessage = message.toLowerCase();
        return lowerMessage.contains("搜索") ||
               lowerMessage.contains("最新") ||
               lowerMessage.contains("今天") ||
               lowerMessage.contains("昨日") ||
               lowerMessage.contains("新闻") ||
               lowerMessage.contains("最近") ||
               lowerMessage.contains("当前") ||
               lowerMessage.contains("实时") ||
               lowerMessage.contains("行情") ||
               lowerMessage.contains("股价") ||
               lowerMessage.contains("天气");
    }

    @Override
    public String chat(String model, String message) {
        LlmModel llmModel = LlmModel.fromValue(model);

        String finalApiKey = getApiKeyForModel(llmModel);
        String finalBaseUrl = getBaseUrlForModel(llmModel);
        String modelName = getModelNameForModel(llmModel);
        return chat(message, finalApiKey, finalBaseUrl, modelName);
    }

    @Override
    public String chat(String message) {
        return chat("deepseek", message);
    }


    private String chat(String message, String apiKey, String baseUrl, String model) {
        OpenAiApi tempApi = OpenAiApi.builder()
                .apiKey(apiKey)
                .baseUrl(baseUrl)
                .build();

        OpenAiChatModel tempChatModel = OpenAiChatModel.builder()
                .openAiApi(tempApi)
                .defaultOptions(OpenAiChatOptions.builder()
                        .model(model)
                        .temperature(0.7)
                        .build())
                .build();

        Prompt prompt = new Prompt(new UserMessage(message));
        ChatResponse response = tempChatModel.call(prompt);
        return extractContent(response);
    }

    private String getApiKeyForModel(LlmModel model) {
        return switch (model) {
            case DEEPSEEK -> deepSeekApiKey != null && !deepSeekApiKey.isEmpty()
                    ? deepSeekApiKey : openAiApiKey;
            case GLM -> glmApiKey != null && !glmApiKey.isEmpty()
                    ? glmApiKey : openAiApiKey;
            case QWEN -> dashScopeApiKey != null && !dashScopeApiKey.isEmpty()
                    ? dashScopeApiKey : openAiApiKey;
            case OPENAI -> openAiApiKey;
            default -> throw new UnsupportedOperationException("Unsupported model for API key: " + model);
        };
    }

    private String getBaseUrlForModel(LlmModel model) {
        return switch (model) {
            case DEEPSEEK -> deepSeekBaseUrl;
            case GLM -> glmBaseUrl;
            case QWEN -> dashScopeBaseUrl;
            case OPENAI -> openAiBaseUrl;
            default -> throw new UnsupportedOperationException("Unsupported model for base URL: " + model);
        };
    }

    private String getModelNameForModel(LlmModel model) {
        return switch (model) {
            case DEEPSEEK -> "deepseek-chat";
            case GLM -> "glm-4";
            case QWEN -> "qwen-turbo";
            case OPENAI -> "gpt-3.5-turbo";
            default -> throw new UnsupportedOperationException("Unsupported model for model name: " + model);
        };
    }

    private String getKeySource(LlmModel model) {
        return switch (model) {
            case DEEPSEEK -> "deepseek";
            case GLM -> "glm";
            case QWEN -> "dashscope";
            case OPENAI -> "openai";
            default -> throw new UnsupportedOperationException("Unsupported model for key source: " + model);
        };
    }

    private String extractContent(ChatResponse response) {
        if (response != null &&
                response.getResults() != null &&
                !response.getResults().isEmpty()) {
            String text = response.getResults().get(0).getOutput().getText();
            return filterThinkingProcess(text);
        }
        return "";
    }

    /**
     * 过滤 DeepSeek 思考过程标签
     * DeepSeek 模型会在响应中包含 <think>...</think> 标签，需要过滤掉
     */
    private String filterThinkingProcess(String text) {
        if (text == null || text.isEmpty()) {
            return text;
        }
        return text.replaceAll("<think>[\\s\\S]*?</think>", "")
                .replaceAll("\\[\\[Final Answer\\]\\]", "")
                .trim();
    }


    //

    @Override
    public Flux<ServerSentEvent<StreamChunk>> smartStream(String model, String message) {
        return smartStream(model, message, null);
    }

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
        String searchResults;
        try {
            searchResults = mcpSearch(message);
        } catch (Exception e) {
            log.error("Search failed, falling back to default stream: {}", e.getMessage());
            return smartStreamDefault(model, message);
        }
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
                        ServerSentEvent.<StreamChunk>builder()
                                .data(new StreamChunk("done", "", null))
                                .event("complete")
                                .build()
                ));
    }

    @Override
    public String mcpSearch(String message) {
        // 注意：这是一个阻塞调用，因为 mcpSearch 本身是同步接口设计（返回 String 而非 Flux）
        // 在 streamChatWithSearch 中调用时，搜索结果会作为流式响应的前缀返回给用户，
        // 因此用户感知到的延迟主要是搜索时间，这是可接受的权衡
        return chatClientRouter.get(LlmModel.MINIMAX.getValue()).prompt()
                .user("请使用联网搜索工具搜索以下信息：" + message)
                .call()
                .content();
    }

    /**
     * Markdown 感知聚合器
     * 保持格式完整性：代码块、表格、列表不截断
     */
    private Flux<StreamChunk> markdownAwareAggregator(Flux<String> source) {

        AtomicReference<ParseState> state = new AtomicReference<>(new ParseState());

        return source.concatMap(token -> {
            ParseState s = state.get();
            s.buffer.append(token);

            List<StreamChunk> chunks = new ArrayList<>();
            String content = s.buffer.toString();

            // 检测代码块边界
            if (content.contains("```")) {
                int fenceCount = countOccurrences(content, "```");
                if (fenceCount % 2 == 0 && fenceCount > 0) {
                    // 完整代码块，可以发送
                    chunks.add(createChunk(content, detectFormat(content)));
                    s.buffer.setLength(0);
                }
            }
            // 检测表格行（多行结构）
            else if (TABLE_ROW.matcher(content).find()) {
                if (content.contains("\n") && content.lines().count() >= 2) {
                    chunks.add(createChunk(content, "table"));
                    s.buffer.setLength(0);
                }
            }
            // 检测列表项
            else if (LIST_ITEM.matcher(content).find() && content.endsWith("\n")) {
                chunks.add(createChunk(content, "list"));
                s.buffer.setLength(0);
            }
            // 检测标题
            else if (HEADER.matcher(content).find() && content.endsWith("\n")) {
                chunks.add(createChunk(content, "heading"));
                s.buffer.setLength(0);
            }
            // 普通段落：按句子边界或长度刷新
            else if (shouldFlushParagraph(content)) {
                chunks.add(createChunk(content, "paragraph"));
                s.buffer.setLength(0);
            }

            return Flux.fromIterable(chunks);

        }).concatWith(Flux.defer(() -> {
            // 发送剩余内容
            ParseState s = state.get();
            if (!s.buffer.isEmpty()) {
                return Flux.just(createChunk(s.buffer.toString(), "text"));
            }
            return Flux.empty();
        }));
    }

    private boolean shouldFlushParagraph(String content) {
        // 中文标点 + 换行，或累积超过 100 字符
        return content.matches(".*[。！？\\n]$") || content.length() > 100;
    }

    private StreamChunk createChunk(String content, String format) {
        return new StreamChunk("content", content, format);
    }

    private String detectFormat(String content) {
        if (content.startsWith("```")) {
            String lang = content.substring(3, content.indexOf('\n')).trim();
            return "code:" + (lang.isEmpty() ? "text" : lang);
        }
        return "text";
    }

    private int countOccurrences(String str, String sub) {
        return str.split(sub, -1).length - 1;
    }

    // 状态持有类
    private static class ParseState {
        StringBuilder buffer = new StringBuilder();
    }


}
