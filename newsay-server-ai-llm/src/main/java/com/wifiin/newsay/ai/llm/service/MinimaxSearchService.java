package com.wifiin.newsay.ai.llm.service;

import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.tool.ToolCallbackProvider;
import org.springframework.stereotype.Service;
import org.springframework.util.MimeTypeUtils;

import java.net.MalformedURLException;
import java.net.URL;

//@Service
public class MinimaxSearchService {

    private final ChatClient chatClient;

    public MinimaxSearchService(
            ChatClient.Builder chatClientBuilder,
            ToolCallbackProvider minimaxTools) {

        this.chatClient = chatClientBuilder
                .defaultTools(minimaxTools)  // 注入 Minimax 工具
                .build();
    }

    /**
     * 使用 Minimax 联网搜索回答问题
     */
    public String searchAndAnswer(String question) {
        return chatClient.prompt()
                .user(question)
                .call()
                .content();
    }

    /**
     * 显式调用搜索（如果需要单独使用）
     */
    public String explicitSearch(String query) {
        return chatClient.prompt()
                .user("请搜索以下信息：" + query)
                .tools("web_search")  // 指定使用 Minimax 的 web_search 工具
                .call()
                .content();
    }

    /**
     * 图片理解（如果问题涉及图片）
     */
    public String understandImage(String imageUrl, String question) throws MalformedURLException {
        URL url = new URL(imageUrl);
        return chatClient.prompt()
                .user(u -> u
                        .text("请分析这张图片：" + question)
                        .media(MimeTypeUtils.parseMimeType("image/png"), url)
                )
                .call()
                .content();
    }
}
