package com.wifiin.newsay.ai.llm.service.impl;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.wifiin.newsay.ai.llm.service.ChatMemoryPersistence;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.ai.chat.messages.AssistantMessage;
import org.springframework.ai.chat.messages.Message;
import org.springframework.ai.chat.messages.UserMessage;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;

import java.sql.Timestamp;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

@Service
public class ChatMemoryPersistenceImpl implements ChatMemoryPersistence {

    private static final Logger log = LoggerFactory.getLogger(ChatMemoryPersistenceImpl.class);
    private static final ObjectMapper objectMapper = new ObjectMapper();

    private final JdbcTemplate jdbcTemplate;

    public ChatMemoryPersistenceImpl(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    @Override
    public void saveToDatabase(String conversationId, List<Message> messages) {
        try {
            // 转换为 JSON 格式：{"role":"user","content":"..."}
            List<java.util.Map<String, String>> msgList = new ArrayList<>();
            for (Message msg : messages) {
                msgList.add(java.util.Map.of(
                        "role", msg instanceof UserMessage ? "user" : "assistant",
                        "content", msg.getText()
                ));
            }
            String messagesJson = objectMapper.writeValueAsString(msgList);

            String sql = """
                INSERT INTO chat_conversation (conversation_id, messages, last_access_time, created_at, updated_at)
                VALUES (?, ?, ?, NOW(), NOW())
                ON DUPLICATE KEY UPDATE messages = ?, last_access_time = ?, updated_at = NOW()
                """;

            jdbcTemplate.update(sql, conversationId, messagesJson, LocalDateTime.now(), messagesJson, LocalDateTime.now());
            log.debug("Saved conversation {} to database with {} messages", conversationId, messages.size());
        } catch (JsonProcessingException e) {
            log.error("Failed to serialize messages for conversation {}", conversationId, e);
        }
    }

    @Override
    public Optional<List<Message>> loadFromDatabase(String conversationId) {
        try {
            String sql = "SELECT messages FROM chat_conversation WHERE conversation_id = ?";
            List<String> results = jdbcTemplate.queryForList(sql, String.class, conversationId);

            if (results.isEmpty()) {
                return Optional.empty();
            }

            String messagesJson = results.get(0);
            List<java.util.Map<String, String>> msgList = objectMapper.readValue(
                    messagesJson,
                    new TypeReference<List<java.util.Map<String, String>>>() {}
            );

            List<Message> messages = new ArrayList<>();
            for (java.util.Map<String, String> msg : msgList) {
                String role = msg.get("role");
                String content = msg.get("content");
                if ("user".equals(role)) {
                    messages.add(new UserMessage(content));
                } else {
                    messages.add(new AssistantMessage(content));
                }
            }

            log.debug("Loaded conversation {} from database with {} messages", conversationId, messages.size());
            return Optional.of(messages);
        } catch (Exception e) {
            log.error("Failed to load messages for conversation {}", conversationId, e);
            return Optional.empty();
        }
    }
}
