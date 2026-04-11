package com.wifiin.newsay.ai.llm.service;

import io.modelcontextprotocol.client.McpClient;
import io.modelcontextprotocol.client.McpSyncClient;
import io.modelcontextprotocol.client.transport.ServerParameters;
import io.modelcontextprotocol.client.transport.StdioClientTransport;
import org.springframework.ai.tool.ToolCallbackProvider;
import org.springframework.ai.tool.method.MethodToolCallbackProvider;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.Map;

@Configuration
public class MinimaxMcpConfig {

    @Bean(destroyMethod = "close")
    public McpSyncClient minimaxMcpClient() {
        // 从环境变量获取配置
        String apiKey = System.getenv("MINIMAX_API_KEY");
        String apiHost = System.getenv("MINIMAX_API_HOST");

        if (apiKey == null || apiKey.isBlank()) {
            throw new IllegalStateException(
                    "缺少 MINIMAX_API_KEY 环境变量，请从 https://platform.minimaxi.com/ 获取"
            );
        }

        // 根据你的账号区域选择正确的 Host
        // 国内用户：https://api.minimaxi.com
        // 国际用户：https://api.minimax.io
        if (apiHost == null || apiHost.isBlank()) {
            apiHost = "https://api.minimaxi.com"; // 默认国内节点
        }

        // 创建 STDIO 传输层 - 启动 Minimax MCP 服务器进程
        ServerParameters params = ServerParameters.builder("uvx")
                .args("minimax-coding-plan-mcp", "-y")
                .env(Map.of(
                        "MINIMAX_API_KEY", apiKey,
                        "MINIMAX_API_HOST", apiHost
                ))
                .build();

        StdioClientTransport transport = new StdioClientTransport(params);

        // 创建同步 MCP 客户端
        McpSyncClient client = McpClient.sync(transport).build();

        // 初始化连接（可选：验证服务器是否正常）
        client.initialize();

        return client;
    }

    @Bean
    public ToolCallbackProvider minimaxTools(McpSyncClient minimaxMcpClient) {
        // 将 Minimax MCP 工具转换为 Spring AI 可用工具
        MethodToolCallbackProvider.builder().toolObjects().
        return McpToolUtils.mcpToolCallbacks(minimaxMcpClient);
    }
}
