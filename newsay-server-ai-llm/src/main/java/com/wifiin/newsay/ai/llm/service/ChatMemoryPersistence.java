package com.wifiin.newsay.ai.llm.service;

import org.springframework.ai.chat.messages.Message;

import java.util.List;
import java.util.Optional;

/**
 * Chat Memory 持久化接口
 * 用于将对话历史持久化到 MySQL
 */
public interface ChatMemoryPersistence {

    /**
     * 保存对话到数据库
     */
    void saveToDatabase(String conversationId, List<Message> messages);

    /**
     * 从数据库加载对话
     */
    Optional<List<Message>> loadFromDatabase(String conversationId);
}
