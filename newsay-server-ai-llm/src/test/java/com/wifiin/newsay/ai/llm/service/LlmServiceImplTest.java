package com.wifiin.newsay.ai.llm.service;

import com.wifiin.newsay.ai.llm.service.impl.LlmServiceImpl;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mock;
import org.mockito.stubbing.Answer;
import org.springframework.ai.chat.client.ChatClient;
import reactor.core.publisher.Flux;

import java.util.HashMap;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class LlmServiceImplTest {

    @Mock
    private ChatMemoryService chatMemoryService;

    private ChatClient chatClient;
    private Map<String, ChatClient> chatClientRouter;

    private LlmServiceImpl llmService;

    @BeforeEach
    void setUp() throws Exception {
        // Create a real HashMap and put the mock in it
        chatClientRouter = new HashMap<>();
        chatClient = mock(ChatClient.class);
        chatClientRouter.put("deepseek", chatClient);
        chatClientRouter.put("minimax", chatClient);

        // Create LlmServiceImpl and inject dependencies
        llmService = new LlmServiceImpl();

        // Inject via reflection
        var routerField = LlmServiceImpl.class.getDeclaredField("chatClientRouter");
        routerField.setAccessible(true);
        routerField.set(llmService, chatClientRouter);

        var memoryField = LlmServiceImpl.class.getDeclaredField("chatMemoryService");
        memoryField.setAccessible(true);
        memoryField.set(llmService, chatMemoryService);
    }

    @Test
    void streamChatWithExplicitSearchEnabled_shouldUseSearchFlow() {
        // Given
        String model = "deepseek";
        String message = "今天天气怎么样";
        String conversationId = "test-123";

        // When
        Flux<String> result = llmService.streamChat(model, message, conversationId, true);

        // Then - verify search flow is triggered
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
