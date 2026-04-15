package com.wifiin.newsay.ai.llm.service;


/*
 * 创建人：baimiao
 * 创建时间：2026/4/10 10:14
 *
 */


import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

@Component
@ConfigurationProperties(prefix = "spring.ai")
public class LLMProperties {


    private ProviderConfig deepseek = new ProviderConfig();
    private ProviderConfig qwen = new ProviderConfig();
    private ProviderConfig glm = new ProviderConfig();
    private ProviderConfig minimax = new ProviderConfig();

    public ProviderConfig getDeepseek() {
        return deepseek;
    }

    public void setDeepseek(ProviderConfig deepseek) {
        this.deepseek = deepseek;
    }

    public ProviderConfig getQwen() {
        return qwen;
    }

    public void setQwen(ProviderConfig qwen) {
        this.qwen = qwen;
    }

    public ProviderConfig getGlm() {
        return glm;
    }

    public void setGlm(ProviderConfig glm) {
        this.glm = glm;
    }

    public ProviderConfig getMinimax() {
        return minimax;
    }

    public void setMinimax(ProviderConfig minimax) {
        this.minimax = minimax;
    }

    public static class ProviderConfig {
        private String apiKey;
        private String baseUrl;
        private String model;
        private Double temperature = 0.7;
        private boolean mcp = false; // 默认不支持MCP

        public String getApiKey() {
            return apiKey;
        }

        public void setApiKey(String apiKey) {
            this.apiKey = apiKey;
        }

        public String getBaseUrl() {
            return baseUrl;
        }

        public void setBaseUrl(String baseUrl) {
            this.baseUrl = baseUrl;
        }

        public String getModel() {
            return model;
        }

        public void setModel(String model) {
            this.model = model;
        }

        public Double getTemperature() {
            return temperature;
        }

        public void setTemperature(Double temperature) {
            this.temperature = temperature;
        }

        public boolean isMcp() {
            return mcp;
        }

        public void setMcp(boolean mcp) {
            this.mcp = mcp;
        }

        public boolean isValid() {
            return apiKey != null && !apiKey.isEmpty()
                    && baseUrl != null && !baseUrl.isEmpty();
        }

        @Override
        public String toString() {
            return "ProviderConfig{" +
                    "apiKey='" + apiKey + '\'' +
                    ", baseUrl='" + baseUrl + '\'' +
                    ", model='" + model + '\'' +
                    ", temperature=" + temperature +
                    '}';
        }
    }

    public ProviderConfig getProvider(String name) {
        return switch (name) {
            case "deepseek" -> deepseek;
            case "qwen" -> qwen;
            case "glm" -> glm;
            case "minimax"->minimax;
            default -> null;
        };
    }
}
