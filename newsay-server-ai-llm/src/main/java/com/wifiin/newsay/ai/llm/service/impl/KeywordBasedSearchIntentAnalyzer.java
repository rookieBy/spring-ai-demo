package com.wifiin.newsay.ai.llm.service.impl;

import com.wifiin.newsay.ai.llm.service.SearchIntentAnalyzer;
import org.springframework.stereotype.Component;

/**
 * Keyword-based search intent analyzer.
 * This is a simple implementation that detects search queries based on keyword matching.
 */
@Component
public class KeywordBasedSearchIntentAnalyzer implements SearchIntentAnalyzer {

    @Override
    public boolean needsSearch(String message) {
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
}
