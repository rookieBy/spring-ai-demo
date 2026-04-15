package com.wifiin.newsay.ai.llm.service.impl;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.wifiin.newsay.ai.llm.entity.ChatConversation;
import com.wifiin.newsay.ai.llm.repository.ChatConversationRepository;
import com.wifiin.newsay.ai.llm.service.ChatMemoryPersistence;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.ai.chat.messages.AssistantMessage;
import org.springframework.ai.chat.messages.Message;
import org.springframework.ai.chat.messages.UserMessage;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

@Service
public class ChatMemoryPersistenceImpl implements ChatMemoryPersistence {

    private static final Logger log = LoggerFactory.getLogger(ChatMemoryPersistenceImpl.class);
    private static final ObjectMapper objectMapper = new ObjectMapper();

    private final ChatConversationRepository repository;

    public ChatMemoryPersistenceImpl(ChatConversationRepository repository) {
        this.repository = repository;
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

            repository.upsert(conversationId, messagesJson, LocalDateTime.now());
            log.debug("Saved conversation {} to database with {} messages", conversationId, messages.size());
        } catch (JsonProcessingException e) {
            log.error("Failed to serialize messages for conversation {}", conversationId, e);
        }
    }

    @Override
    public Optional<List<Message>> loadFromDatabase(String conversationId) {
        return repository.findByConversationId(conversationId)
                .map(entity -> {
                    try {
                        List<java.util.Map<String, String>> msgList = objectMapper.readValue(
                                entity.getMessages(),
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
                        return messages;
                    } catch (JsonProcessingException e) {
                        log.error("Failed to deserialize messages for conversation {}", conversationId, e);
                        return new ArrayList<Message>();
                    }
                });
    }
}
