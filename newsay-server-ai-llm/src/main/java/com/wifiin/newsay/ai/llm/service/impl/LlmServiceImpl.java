package com.wifiin.newsay.ai.llm.service.impl;

import com.wifiin.newsay.ai.llm.enums.LlmModel;
import com.wifiin.newsay.ai.llm.model.StreamChunk;
import com.wifiin.newsay.ai.llm.service.LlmService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.ai.chat.client.ChatClient;
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
        return streamChat("deepseek", message);
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
        };
    }

    private String getBaseUrlForModel(LlmModel model) {
        return switch (model) {
            case DEEPSEEK -> deepSeekBaseUrl;
            case GLM -> glmBaseUrl;
            case QWEN -> dashScopeBaseUrl;
            case OPENAI -> openAiBaseUrl;
        };
    }

    private String getModelNameForModel(LlmModel model) {
        return switch (model) {
            case DEEPSEEK -> "deepseek-chat";
            case GLM -> "glm-4";
            case QWEN -> "qwen-turbo";
            case OPENAI -> "gpt-3.5-turbo";
        };
    }

    private String getKeySource(LlmModel model) {
        return switch (model) {
            case DEEPSEEK -> "deepseek";
            case GLM -> "glm";
            case QWEN -> "dashscope";
            case OPENAI -> "openai";
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
