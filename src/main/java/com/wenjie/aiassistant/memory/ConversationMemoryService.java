package com.wenjie.aiassistant.memory;

import com.wenjie.aiassistant.dto.ChatMessageDTO;
import com.wenjie.aiassistant.dto.ConversationListItemResponse;

import java.util.List;

public interface ConversationMemoryService {


    /**
     * 获取最近 N 条消息
     */
    List<ChatMessageDTO> getRecentMessages(String conversationId, int limit);



    /**
     * 获取下一个消息编号
     */
    int nextMessageIndex(String conversationId);

    /**
     * 获取当前最新消息编号
     */
    int getCurrentMessageIndex(String conversationId);

    /**
     * 获取 summary
     */
    String getSummary(String conversationId);

    /**
     * 更新 summary
     */
    void updateSummary(String conversationId, String summary);

    /**
     * 获取上次 summary 更新到哪个 messageIndex
     */
    int getLastSummaryMessageIndex(String conversationId);

    /**
     * 更新上次 summary 的 messageIndex
     */
    void updateLastSummaryMessageIndex(String conversationId, int messageIndex);

    /**
     * 获取标题
     */
    String getTitle(String conversationId);

    /**
     * 更新标题
     */
    void updateTitle(String conversationId, String title);

    /**
     * 当前会话是否已有标题
     */
    boolean hasTitle(String conversationId);

    /**
     * 清空当前会话全部内存状态
     */
    void clear(String conversationId);

    /**
     * 当前会话是否已经加载到 Memory
     */
    boolean exists(String conversationId);

    /**
     * 从数据库恢复完整运行状态到 Memory
     */
    void restoreConversation(String conversationId, String title, String summary, int currentMessageIndex, int lastSummaryMessageIndex, List<ChatMessageDTO> messages);

    /**
     * 查询内存中的会话列表
     */
    List<ConversationListItemResponse> listConversations();

    /**
     * 刷新会话创建/活跃时间
     */
    void touchConversation(String conversationId);
}