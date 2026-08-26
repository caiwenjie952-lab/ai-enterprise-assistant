package com.wenjie.aiassistant.memory;

import com.wenjie.aiassistant.dto.ChatMessageDTO;

import java.util.List;

public interface ConversationMemoryService {

    /**
     * 获取某个会话的全部内存消息
     */
    List<ChatMessageDTO> getMessages(String conversationId);

    /**
     * 获取最近 N 条消息
     */
    List<ChatMessageDTO> getRecentMessages(String conversationId, int limit);

    /**
     * 获取 messageIndex 大于指定值的消息
     */
    List<ChatMessageDTO> getMessagesAfterIndex(String conversationId, int messageIndex, int limit);

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
     * 获取会话摘要
     */
    String getSummary(String conversationId);

    /**
     * 保存/更新会话摘要
     */
    void updateSummary(String conversationId, String summary);

    /**
     * 获取会话内存消息数量
     */
    int countMessages(String conversationId);

    /**
     * 生成下一个消息序号
     */
    int nextMessageIndex(String conversationId);

    /**
     * 获取当前会话最新消息序号
     */
    int getCurrentMessageIndex(String conversationId);

    /**
     * 获取上一次生成摘要时的消息序号
     */
    int getLastSummaryMessageIndex(String conversationId);

    /**
     * 更新上一次生成摘要时的消息序号
     */
    void updateLastSummaryMessageIndex(String conversationId, int messageIndex);

    /**
     * 裁剪内存消息，只保留最近 limit 条
     */
    void trimMessages(String conversationId, int limit);

    /**
     * 获取会话标题
     */
    String getTitle(String conversationId);

    /**
     * 更新会话标题
     */
    void updateTitle(String conversationId, String title);

    /**
     * 判断会话是否已有标题
     */
    boolean hasTitle(String conversationId);
}