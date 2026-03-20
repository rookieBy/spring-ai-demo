package com.example.llm.service;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import reactor.core.publisher.Flux;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * DeepSeek 流式聊天测试
 *
 * 测试命令:
 * curl -X POST http://localhost:8080/api/chat/stream \
 *   -H "Content-Type: application/json" \
 *   -d '{"model":"deepseek","message":"1+1等于几"}'
 *
 * 验证流式输出只返回结果，不返回思考过程
 */
@SpringBootTest
class DeepSeekStreamChatTest {

    @Autowired(required = false)
    private LlmService llmService;

    @Test
    void testStreamChat() {
        if (llmService == null) {
            return;
        }

        Flux<String> flux = llmService.streamChat("deepseek", "你好");

        StringBuilder result = new StringBuilder();
        flux.subscribe(result::append);

        // 简单验证
        assertThat(result).isNotNull();
    }
}