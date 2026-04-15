package com.wifiin.newsay.ai.llm.repository;

import com.wifiin.newsay.ai.llm.entity.ChatConversation;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.Optional;

@Repository
public interface ChatConversationRepository extends JpaRepository<ChatConversation, Long> {

    Optional<ChatConversation> findByConversationId(String conversationId);

    @Modifying
    @Query(value = "INSERT INTO chat_conversation (conversation_id, messages, last_access_time, created_at, updated_at) " +
            "VALUES (:conversationId, :messages, :lastAccessTime, NOW(), NOW()) " +
            "ON DUPLICATE KEY UPDATE messages = :messages, last_access_time = :lastAccessTime, updated_at = NOW()",
            nativeQuery = true)
    void upsert(@Param("conversationId") String conversationId,
                @Param("messages") String messages,
                @Param("lastAccessTime") LocalDateTime lastAccessTime);
}