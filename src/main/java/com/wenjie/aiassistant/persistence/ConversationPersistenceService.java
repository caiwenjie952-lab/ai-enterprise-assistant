package com.wenjie.aiassistant.persistence;

import com.wenjie.aiassistant.dto.ChatMessageDTO;
import com.wenjie.aiassistant.dto.ConversationDetailResponse;
import com.wenjie.aiassistant.dto.ConversationListItemResponse;
import com.wenjie.aiassistant.entity.AiConversationEntity;

import java.util.List;

public interface ConversationPersistenceService {

    /**
     * 确保数据库中存在当前会话
     */
    void ensureConversation(String conversationId);

    /**
     * 保存消息
     */
    void saveMessages(String conversationId, List<ChatMessageDTO> messages);

    /**
     * 更新标题
     */
    void updateTitle(String conversationId, String title);

    /**
     * 更新摘要和摘要进度
     */
    void updateSummary(String conversationId, String summary, int lastSummaryMessageIndex);

    /**
     * 更新当前消息序号
     */
    void updateCurrentMessageIndex(String conversationId, int currentMessageIndex);

    /**
     * 根据 conversationId 查询会话
     */
    AiConversationEntity findConversation(String conversationId);

    /**
     * 查询最近 N 条数据库消息
     */
    List<ChatMessageDTO> findRecentMessages(String conversationId, int limit);

    List<ConversationListItemResponse> listConversations();

    ConversationDetailResponse getConversationDetail(String conversationId);
}