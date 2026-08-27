package com.wenjie.aiassistant.conversation.impl;

import com.wenjie.aiassistant.config.AiProperties;
import com.wenjie.aiassistant.conversation.ConversationRestoreService;
import com.wenjie.aiassistant.dto.ChatMessageDTO;
import com.wenjie.aiassistant.entity.AiConversationEntity;
import com.wenjie.aiassistant.memory.ConversationMemoryService;
import com.wenjie.aiassistant.persistence.ConversationPersistenceService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
public class ConversationRestoreServiceImpl implements ConversationRestoreService {

    private final ConversationMemoryService conversationMemoryService;

    private final ConversationPersistenceService conversationPersistenceService;

    private final AiProperties aiProperties;


    @Override
    public boolean ensureLoaded(String conversationId) {
        if (conversationId == null || conversationId.isBlank()) {
            return false;
        }

        // Memory 已经存在
        if (conversationMemoryService.exists(conversationId)) {
            return true;
        }

        // Memory 不存在，查 MySQL
        AiConversationEntity conversation = conversationPersistenceService.findConversation(conversationId);

        if (conversation == null) {
            log.info("数据库中不存在会话，conversationId={}", conversationId);

            return false;
        }

        int maxMemoryMessages = aiProperties.getMaxMemoryMessages() == null ? 50 : aiProperties.getMaxMemoryMessages();

        List<ChatMessageDTO> recentMessages = conversationPersistenceService.findRecentMessages(conversationId, maxMemoryMessages);

        int currentMessageIndex = conversation.getCurrentMessageIndex() == null ? 0 : conversation.getCurrentMessageIndex();

        int lastSummaryMessageIndex = conversation.getLastSummaryMessageIndex() == null ? 0 : conversation.getLastSummaryMessageIndex();

        conversationMemoryService.restoreConversation(conversationId, conversation.getTitle(), conversation.getSummary(), currentMessageIndex, lastSummaryMessageIndex, recentMessages);

        log.info("会话从 MySQL 恢复成功，conversationId={}，currentMessageIndex={}，lastSummaryMessageIndex={}，恢复消息数={}", conversationId, currentMessageIndex, lastSummaryMessageIndex, recentMessages.size());

        return true;
    }
}