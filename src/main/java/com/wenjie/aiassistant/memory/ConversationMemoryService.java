package com.wenjie.aiassistant.memory;

import com.wenjie.aiassistant.dto.ChatMessageDTO;

import java.util.List;

public interface ConversationMemoryService {

    /**
     * 获取某个会话的历史消息
     */
    List<ChatMessageDTO> getMessages(String conversationId);

    /**
     * 添加一条消息
     */
    void addMessage(String conversationId, ChatMessageDTO message);

    /**
     * 添加多条消息
     */
    void addMessages(String conversationId, List<ChatMessageDTO> messages);

    /**
     * 清空某个会话
     */
    void clear(String conversationId);

    /**
     * 获取最近 N 条消息
     */
    List<ChatMessageDTO> getRecentMessages(String conversationId, int limit);
}