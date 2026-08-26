package com.wenjie.aiassistant.service;

import com.wenjie.aiassistant.dto.ChatMessageDTO;

import java.util.List;

public interface ConversationPersistenceService {

    /**
     * 确保会话存在。
     */
    void ensureConversation(String conversationId);

    /**
     * 保存消息。
     */
    void saveMessages(
            String conversationId,
            List<ChatMessageDTO> messages
    );

    /**
     * 更新标题。
     */
    void updateTitle(
            String conversationId,
            String title
    );

    /**
     * 更新摘要和摘要进度。
     */
    void updateSummary(
            String conversationId,
            String summary,
            int lastSummaryMessageIndex
    );

    /**
     * 更新当前最新消息序号。
     */
    void updateCurrentMessageIndex(
            String conversationId,
            int currentMessageIndex
    );
}
