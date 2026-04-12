//package com.wifiin.newsay.ai.llm.service;
//
//import io.modelcontextprotocol.client.McpClient;
//import io.modelcontextprotocol.client.McpSyncClient;
//import io.modelcontextprotocol.client.transport.ServerParameters;
//import io.modelcontextprotocol.client.transport.StdioClientTransport;
//import org.springframework.ai.mcp.SyncMcpToolCallbackProvider;
//import org.springframework.ai.tool.ToolCallbackProvider;
//import org.springframework.beans.factory.annotation.Value;
//import org.springframework.context.annotation.Bean;
//import org.springframework.context.annotation.Configuration;
//
//import java.util.Map;
//
////@Configuration
//public class MinimaxMcpConfig {
//
//
//    @Value("${spring.ai.minimax.api-key}")
//    private String apiKey;
//    @Value("${spring.ai.minimax.host}")
//    private String apiHost;
//
//
//    @Bean(destroyMethod = "close")
//    public McpSyncClient minimaxMcpClient() {
//
//        if (apiKey == null || apiKey.isBlank()) {
//            throw new IllegalStateException(
//                    "缺少 MINIMAX_API_KEY 环境变量，请从 https://platform.minimaxi.com/ 获取"
//            );
//        }
//
//        // 根据你的账号区域选择正确的 Host
//        // 国内用户：https://api.minimaxi.com
//        // 国际用户：https://api.minimax.io
//        if (apiHost == null || apiHost.isBlank()) {
//            apiHost = "https://api.minimaxi.com"; // 默认国内节点
//        }
//
//        // 创建 STDIO 传输层 - 启动 Minimax MCP 服务器进程
//        ServerParameters params = ServerParameters.builder("wsl")
//                .args("uvx","minimax-coding-plan-mcp", "-y")
//                .env(Map.of(
//                        "MINIMAX_API_KEY", apiKey,
//                        "MINIMAX_API_HOST", apiHost
//                ))
//                .build();
//
//        StdioClientTransport transport = new StdioClientTransport(params);
//
//        // 创建同步 MCP 客户端
//        McpSyncClient client = McpClient.sync(transport).build();
//
//        // 初始化连接（可选：验证服务器是否正常）
//        client.initialize();
//
//        return client;
//    }
//
//    @Bean
//    public ToolCallbackProvider minimaxTools(McpSyncClient minimaxMcpClient) {
//        // ✅ 使用 SyncMcpToolCallbackProvider 而不是 McpToolUtils
//        return new SyncMcpToolCallbackProvider(minimaxMcpClient);
//    }
//}
