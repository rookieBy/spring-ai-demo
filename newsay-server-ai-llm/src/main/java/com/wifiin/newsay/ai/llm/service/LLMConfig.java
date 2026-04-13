package com.wifiin.newsay.ai.llm.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import io.modelcontextprotocol.client.McpClient;
import io.modelcontextprotocol.client.McpSyncClient;
import io.modelcontextprotocol.client.transport.ServerParameters;
import io.modelcontextprotocol.client.transport.StdioClientTransport;
import io.modelcontextprotocol.json.jackson.JacksonMcpJsonMapper;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.mcp.SyncMcpToolCallbackProvider;
import org.springframework.ai.openai.OpenAiChatModel;
import org.springframework.ai.openai.OpenAiChatOptions;
import org.springframework.ai.openai.api.OpenAiApi;
import org.springframework.ai.tool.ToolCallbackProvider;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.lang.Nullable;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;

@Configuration
public class LLMConfig {

    @Autowired
    private LLMProperties properties;

    private ChatClient createClient(String providerName, @Nullable ToolCallbackProvider provider) {
        LLMProperties.ProviderConfig cfg = properties.getProvider(providerName);
        System.out.println("configMap:" + cfg);

        // 防御性编程：配置缺失时抛出明确异常
        if (cfg == null) {
            throw new IllegalStateException(
                    "Provider '" + providerName + "' not configured. " +
                            "Available: "
            );
        }

        if (!cfg.isValid()) {
            throw new IllegalStateException(
                    "Provider '" + providerName + "' config invalid. " +
                            "baseUrl=" + cfg.getBaseUrl() + ", apiKey=" + (cfg.getApiKey() != null ? "***" : "null")
            );
        }

        var openAiApi = OpenAiApi.builder()
                .apiKey(cfg.getApiKey())
                .baseUrl(cfg.getBaseUrl())
                .build();
        var chatModel = OpenAiChatModel.builder()
                .openAiApi(openAiApi)
                .build();

        ChatClient.Builder builder = ChatClient.builder(chatModel)
                .defaultOptions(OpenAiChatOptions.builder()
                        .model(cfg.getModel())
                        .temperature(cfg.getTemperature() != null ? cfg.getTemperature() : 0.7)
                        .build());
        if (!Objects.isNull(provider)) {
            builder.defaultToolCallbacks(provider.getToolCallbacks());
        }
        return builder.build();
    }

    @Bean("deepseekChatClient")
    public ChatClient deepseekChatClient() {
        return createClient("deepseek", null);
    }

    @Bean("qwenChatClient")
    public ChatClient qwenChatClient() {
        return createClient("qwen", null);
    }

    @Bean("glmChatClient")
    public ChatClient glmChatClient() {
        return createClient("glm", null);
    }

    @Bean("minimaxChatClient")
    public ChatClient minimaxChatClient(@Qualifier("minimaxMcp") ToolCallbackProvider provider) {
        return createClient("minimax", provider);
    }


    @Bean(destroyMethod = "close")
    public McpSyncClient minimaxMcpClient() {
        LLMProperties.ProviderConfig cfg = properties.getProvider("minimax");

        // 创建 STDIO 传输层 - 启动 Minimax MCP 服务器进程
        ServerParameters params = ServerParameters.builder("uvx")
                .args("minimax-coding-plan-mcp", "-y")
                .env(Map.of(
                        "MINIMAX_API_KEY", cfg.getApiKey(),
                        "MINIMAX_API_HOST", cfg.getBaseUrl()
                ))
                .build();

        StdioClientTransport transport = new StdioClientTransport(params, new JacksonMcpJsonMapper(new ObjectMapper()));

        // 创建同步 MCP 客户端
        McpSyncClient client = McpClient.sync(transport).build();

        // 初始化连接（可选：验证服务器是否正常）
        client.initialize();

        return client;
    }

    @Bean("minimaxMcp")
    public ToolCallbackProvider minimaxTools(McpSyncClient minimaxMcpClient) {
        System.out.println("minimaxTools initial:" + Objects.isNull(minimaxMcpClient));
        ToolCallbackProvider provider = new SyncMcpToolCallbackProvider(minimaxMcpClient);
        return provider;
    }


    @Bean("llmRouter")
    public Map<String, ChatClient> chatClientRouter(
            @Qualifier("deepseekChatClient") ObjectProvider<ChatClient> deepseekProvider,
            @Qualifier("qwenChatClient") ObjectProvider<ChatClient> qwenProvider,
            @Qualifier("glmChatClient") ObjectProvider<ChatClient> glmProvider,
            @Qualifier("minimaxChatClient") ObjectProvider<ChatClient> minimaxProvider
    ) {

        Map<String, ChatClient> router = new HashMap<>();

        // 只有实际创建的 Bean 才加入路由
        deepseekProvider.ifAvailable(client -> router.put("deepseek", client));
        qwenProvider.ifAvailable(client -> router.put("qwen", client));
        glmProvider.ifAvailable(client -> router.put("glm", client));
        minimaxProvider.ifAvailable(client -> router.put("minimax", client));

        if (router.isEmpty()) {
            throw new IllegalStateException("No LLM provider configured!");
        }
        System.out.println("=============" + router);
        return Collections.unmodifiableMap(router);
    }
}
