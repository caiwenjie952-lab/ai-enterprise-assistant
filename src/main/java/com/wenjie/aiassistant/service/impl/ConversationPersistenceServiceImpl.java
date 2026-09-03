package com.wenjie.aiassistant.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.wenjie.aiassistant.dto.ChatMessageDTO;
import com.wenjie.aiassistant.dto.ConversationDetailResponse;
import com.wenjie.aiassistant.dto.ConversationListItemResponse;
import com.wenjie.aiassistant.entity.AiChatMessageEntity;
import com.wenjie.aiassistant.entity.AiConversationEntity;
import com.wenjie.aiassistant.mapper.AiChatMessageMapper;
import com.wenjie.aiassistant.mapper.AiConversationMapper;
import com.wenjie.aiassistant.persistence.ConversationPersistenceService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.Comparator;
import java.util.List;

@Service
@RequiredArgsConstructor
public class ConversationPersistenceServiceImpl implements ConversationPersistenceService {

    private final AiConversationMapper aiConversationMapper;

    private final AiChatMessageMapper aiChatMessageMapper;


    @Override
    public void ensureConversation(String conversationId) {
        AiConversationEntity existing = findConversation(conversationId);

        if (existing != null) {
            return;
        }

        AiConversationEntity entity = new AiConversationEntity();

        entity.setConversationId(conversationId);

        entity.setTitle("新会话");

        entity.setSummary("");

        entity.setCurrentMessageIndex(0);

        entity.setLastSummaryMessageIndex(0);

        aiConversationMapper.insert(entity);
    }


    @Override
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


    @Override
    public AiConversationEntity findConversation(String conversationId) {
        return aiConversationMapper.selectOne(new LambdaQueryWrapper<AiConversationEntity>().eq(AiConversationEntity::getConversationId, conversationId));
    }


    @Override
    public List<ChatMessageDTO> findRecentMessages(String conversationId, int limit) {
        if (limit <= 0) {
            return List.of();
        }

        List<AiChatMessageEntity> entities = aiChatMessageMapper.selectList(new LambdaQueryWrapper<AiChatMessageEntity>().eq(AiChatMessageEntity::getConversationId, conversationId).orderByDesc(AiChatMessageEntity::getMessageIndex).last("LIMIT " + limit));

        return entities.stream().sorted(Comparator.comparing(AiChatMessageEntity::getMessageIndex)).map(entity -> new ChatMessageDTO(entity.getMessageIndex(), entity.getRole(), entity.getContent())).toList();
    }

    @Override
    public List<ConversationListItemResponse> listConversations() {
        List<AiConversationEntity> conversations = aiConversationMapper.selectList(new LambdaQueryWrapper<AiConversationEntity>().orderByDesc(AiConversationEntity::getUpdateTime));

        return conversations.stream().map(conversation -> {
            String conversationId = conversation.getConversationId();

            Long messageCount = aiChatMessageMapper.selectCount(new LambdaQueryWrapper<AiChatMessageEntity>().eq(AiChatMessageEntity::getConversationId, conversationId));

            AiChatMessageEntity lastMessageEntity = aiChatMessageMapper.selectOne(new LambdaQueryWrapper<AiChatMessageEntity>().eq(AiChatMessageEntity::getConversationId, conversationId).orderByDesc(AiChatMessageEntity::getMessageIndex).last("LIMIT 1"));

            String lastMessage = lastMessageEntity == null ? null : lastMessageEntity.getContent();

            return new ConversationListItemResponse(conversationId, conversation.getTitle(), messageCount.intValue(), conversation.getCurrentMessageIndex(), conversation.getLastSummaryMessageIndex(), conversation.getSummary() != null && !conversation.getSummary().isBlank(), lastMessage, toTimestamp(conversation.getCreateTime()), toTimestamp(conversation.getUpdateTime()));
        }).toList();
    }

    @Override
    public ConversationDetailResponse getConversationDetail(String conversationId) {
        AiConversationEntity conversation = findConversation(conversationId);

        if (conversation == null) {
            return null;
        }

        List<AiChatMessageEntity> messageEntities = aiChatMessageMapper.selectList(new LambdaQueryWrapper<AiChatMessageEntity>().eq(AiChatMessageEntity::getConversationId, conversationId).orderByAsc(AiChatMessageEntity::getMessageIndex));

        List<ChatMessageDTO> messages = messageEntities.stream().map(message -> new ChatMessageDTO(message.getMessageIndex(), message.getRole(), message.getContent())).toList();

        return new ConversationDetailResponse(conversationId, conversation.getTitle(), conversation.getSummary(), messages.size(), conversation.getCurrentMessageIndex(), conversation.getLastSummaryMessageIndex(), messages);
    }

    @Override
    @Transactional
    public boolean deleteConversation(String conversationId) {
        AiConversationEntity conversation = findConversation(conversationId);

        if (conversation == null) {
            return false;
        }

        aiChatMessageMapper.delete(new LambdaQueryWrapper<AiChatMessageEntity>().eq(AiChatMessageEntity::getConversationId, conversationId));

        aiConversationMapper.delete(new LambdaQueryWrapper<AiConversationEntity>().eq(AiConversationEntity::getConversationId, conversationId));

        return true;
    }

    @Override
    public List<ChatMessageDTO> findMessagesAfterIndex(String conversationId, int messageIndex, int limit) {
        List<AiChatMessageEntity> entities = aiChatMessageMapper.selectList(new LambdaQueryWrapper<AiChatMessageEntity>().
                eq(AiChatMessageEntity::getConversationId, conversationId).gt(AiChatMessageEntity::getMessageIndex, messageIndex).
                orderByAsc(AiChatMessageEntity::getMessageIndex).
                last("LIMIT " + limit));

        return entities.stream().map(entity -> new ChatMessageDTO(entity.getMessageIndex(), entity.getRole(), entity.getContent())).toList();
    }

    private Long toTimestamp(LocalDateTime time) {
        if (time == null) {
            return null;
        }

        return time.atZone(ZoneId.systemDefault()).toInstant().toEpochMilli();
    }
}