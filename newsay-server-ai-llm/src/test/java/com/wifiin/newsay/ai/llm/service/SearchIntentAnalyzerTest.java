package com.wifiin.newsay.ai.llm.service;

import com.wifiin.newsay.ai.llm.service.impl.KeywordBasedSearchIntentAnalyzer;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import static org.junit.jupiter.api.Assertions.*;

class SearchIntentAnalyzerTest {

    private SearchIntentAnalyzer analyzer;

    @BeforeEach
    void setUp() {
        analyzer = new KeywordBasedSearchIntentAnalyzer();
    }

    @ParameterizedTest
    @ValueSource(strings = {
            "今天天气怎么样",
            "最新新闻有哪些",
            "帮我搜索一下",
            "现在股价多少",
            "当前排名是多少"
    })
    void shouldDetectSearchIntent(String message) {
        assertTrue(analyzer.needsSearch(message), "Should detect search intent for: " + message);
    }

    @ParameterizedTest
    @ValueSource(strings = {
            "如何写一个Hello World",
            "给我讲个笑话",
            "翻译这句话",
            "帮我写代码"
    })
    void shouldNotDetectSearchIntent(String message) {
        assertFalse(analyzer.needsSearch(message), "Should not detect search intent for: " + message);
    }

    @Test
    void shouldReturnFalseForNullOrEmpty() {
        assertFalse(analyzer.needsSearch(null));
        assertFalse(analyzer.needsSearch(""));
        assertFalse(analyzer.needsSearch("   "));
    }
}
