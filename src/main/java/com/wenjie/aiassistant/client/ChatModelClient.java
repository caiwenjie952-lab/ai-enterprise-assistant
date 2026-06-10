package com.wenjie.aiassistant.client;

import com.wenjie.aiassistant.dto.ChatMessageDTO;

import java.util.List;

public interface ChatModelClient {

    /**
     * 调用聊天模型
     *
     * @param messages 对话消息列表
     * @return 模型回复
     */
    String chat(List<ChatMessageDTO> messages);
}