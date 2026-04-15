package com.wifiin.newsay.ai.llm.service;

/**
 * Interface for analyzing whether a user message requires web search capability.
 */
public interface SearchIntentAnalyzer {

    /**
     * Analyzes whether the given message requires web search.
     *
     * @param message the user message to analyze
     * @return true if search is needed, false otherwise
     */
    boolean needsSearch(String message);
}
