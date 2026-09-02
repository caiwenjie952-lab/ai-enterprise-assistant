package com.wenjie.aiassistant.conversation.impl;

import com.wenjie.aiassistant.conversation.ConversationService;
import com.wenjie.aiassistant.dto.ConversationDetailResponse;
import com.wenjie.aiassistant.dto.ConversationListItemResponse;
import com.wenjie.aiassistant.exception.BusinessException;
import com.wenjie.aiassistant.memory.ConversationMemoryService;
import com.wenjie.aiassistant.persistence.ConversationPersistenceService;
import lombok.RequiredArgsConstructor;
import org.springframework.ai.chat.memory.ChatMemory;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
@RequiredArgsConstructor
public class ConversationServiceImpl implements ConversationService {

    private final ConversationMemoryService conversationMemoryService;
    private final ConversationPersistenceService conversationPersistenceService;
    private final ChatMemory chatMemory;

    @Override
    public List<ConversationListItemResponse> listConversations() {
        return conversationPersistenceService.listConversations();
    }

    @Override
    public ConversationDetailResponse getConversationDetail(String conversationId) {
        ConversationDetailResponse detail = conversationPersistenceService.getConversationDetail(conversationId);

        if (detail == null) {
            throw new BusinessException(404, "会话不存在");
        }

        return detail;
    }

    @Override
    public void deleteConversation(String conversationId) {
        boolean deleted = conversationPersistenceService.deleteConversation(conversationId);

        if (!deleted) {
            throw new BusinessException(404, "会话不存在");
        }

        conversationMemoryService.clear(conversationId);
        chatMemory.clear(conversationId);
    }
}