package com.wifiin.newsay.ai.llm.task;

import com.wifiin.newsay.ai.llm.service.ChatMemoryPersistence;
import com.wifiin.newsay.ai.llm.service.ChatMemoryService;
import com.wifiin.newsay.ai.llm.service.impl.RedisChatMemoryServiceImpl;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.ai.chat.messages.Message;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.util.Set;

@Component
public class ChatMemorySyncTask {

    private static final Logger log = LoggerFactory.getLogger(ChatMemorySyncTask.class);

    @Autowired
    private ChatMemoryService chatMemoryService;

    @Autowired
    private RedisChatMemoryServiceImpl redisChatMemoryService;

    @Autowired
    private ChatMemoryPersistence chatMemoryPersistence;

    @Scheduled(fixedRateString = "${spring.ai.chat.memory.sync-interval-minutes:10}000")
    public void syncChatMemoryToDatabase() {
        log.info("Starting chat memory sync to database...");

        try {
            Set<String> conversationIds = redisChatMemoryService.getAllConversationIds();
            log.info("Found {} conversations to sync", conversationIds.size());

            int syncedCount = 0;
            for (String conversationId : conversationIds) {
                try {
                    var messages = chatMemoryService.getHistory(conversationId);
                    if (!messages.isEmpty()) {
                        chatMemoryPersistence.saveToDatabase(conversationId, messages);
                        syncedCount++;
                    }
                } catch (Exception e) {
                    log.error("Failed to sync conversation {}", conversationId, e);
                }
            }

            log.info("Chat memory sync completed. Synced {} conversations.", syncedCount);
        } catch (Exception e) {
            log.error("Chat memory sync failed", e);
        }
    }
}
