package com.wenjie.aiassistant.memory;

import com.wenjie.aiassistant.dto.ChatMessageDTO;

import java.util.List;

public interface ConversationMemoryService {

    List<ChatMessageDTO> getMessages(String conversationId);

    List<ChatMessageDTO> getRecentMessages(String conversationId, int limit);

    void addMessage(String conversationId, ChatMessageDTO message);

    void addMessages(String conversationId, List<ChatMessageDTO> messages);

    void clear(String conversationId);

    /**
     * 获取会话摘要
     */
    String getSummary(String conversationId);

    /**
     * 保存/更新会话摘要
     */
    void updateSummary(String conversationId, String summary);

    /**
     * 获取会话消息数量
     */
    int countMessages(String conversationId);
}