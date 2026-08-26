package com.wenjie.aiassistant.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.wenjie.aiassistant.dto.ChatMessageDTO;
import com.wenjie.aiassistant.entity.AiChatMessageEntity;
import com.wenjie.aiassistant.entity.AiConversationEntity;
import com.wenjie.aiassistant.mapper.AiChatMessageMapper;
import com.wenjie.aiassistant.mapper.AiConversationMapper;
import com.wenjie.aiassistant.service.ConversationPersistenceService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
public class ConversationPersistenceServiceImpl implements ConversationPersistenceService {

    private final AiConversationMapper aiConversationMapper;

    private final AiChatMessageMapper aiChatMessageMapper;


    @Override
    public void ensureConversation(String conversationId) {
        Long count = aiConversationMapper.selectCount(new LambdaQueryWrapper<AiConversationEntity>()
                .eq(AiConversationEntity::getConversationId, conversationId));

        if (count != null && count > 0) {
            return;
        }

        AiConversationEntity entity = new AiConversationEntity();
        entity.setConversationId(conversationId);
        entity.setTitle("新会话");
        entity.setSummary("");
        entity.setCurrentMessageIndex(0);
        entity.setLastSummaryMessageIndex(0);

        try {
            aiConversationMapper.insert(entity);
        } catch (org.springframework.dao.DuplicateKeyException ignored) {
            // 并发请求由 conversation_id 唯一索引兜底，已存在时视为成功。
        }
    }

    @Override
    @Transactional
    public void saveMessages(String conversationId, List<ChatMessageDTO> messages) {
        if (messages == null || messages.isEmpty()) {
            return;
        }

        for (ChatMessageDTO message : messages) {
            AiChatMessageEntity entity = new AiChatMessageEntity();

            entity.setConversationId(conversationId);
            entity.setMessageIndex(message.getMessageIndex());
            entity.setRole(message.getRole());
            entity.setContent(message.getContent());

            aiChatMessageMapper.insert(entity);
        }
    }

    @Override
    public void updateTitle(String conversationId, String title) {
        aiConversationMapper.update(null, new LambdaUpdateWrapper<AiConversationEntity>().eq(AiConversationEntity::getConversationId, conversationId).set(AiConversationEntity::getTitle, title));
    }

    @Override
    public void updateSummary(String conversationId, String summary, int lastSummaryMessageIndex) {
        aiConversationMapper.update(null, new LambdaUpdateWrapper<AiConversationEntity>().eq(AiConversationEntity::getConversationId, conversationId).set(AiConversationEntity::getSummary, summary).set(AiConversationEntity::getLastSummaryMessageIndex, lastSummaryMessageIndex));
    }

    @Override
    public void updateCurrentMessageIndex(String conversationId, int currentMessageIndex) {
        aiConversationMapper.update(null, new LambdaUpdateWrapper<AiConversationEntity>().eq(AiConversationEntity::getConversationId, conversationId).set(AiConversationEntity::getCurrentMessageIndex, currentMessageIndex));
    }
}
