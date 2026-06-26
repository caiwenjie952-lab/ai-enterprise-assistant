package com.wenjie.aiassistant.client;

import com.wenjie.aiassistant.dto.ChatMessageDTO;

import java.util.List;

public interface ChatModelClient {

    /**
     * 调用聊天模型
     */
    String chat(List<ChatMessageDTO> messages);

    /**
     * 调用模型生成会话摘要
     */
    String summarize(String oldSummary, List<ChatMessageDTO> messages);
}