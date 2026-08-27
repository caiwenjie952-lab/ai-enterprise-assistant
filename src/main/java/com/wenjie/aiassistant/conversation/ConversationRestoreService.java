package com.wenjie.aiassistant.conversation;

public interface ConversationRestoreService {

    /**
     * 如果当前 Memory 中没有该会话，
     * 尝试从 MySQL 恢复。
     *
     * @return true 表示恢复成功或原本已经存在；
     * false 表示数据库也不存在该会话。
     */
    boolean ensureLoaded(String conversationId);
}