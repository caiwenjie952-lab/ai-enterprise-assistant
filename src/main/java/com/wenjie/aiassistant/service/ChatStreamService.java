package com.wenjie.aiassistant.service;

import com.wenjie.aiassistant.dto.ChatRequest;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

public interface ChatStreamService {

    /**
     * 流式聊天
     */
    SseEmitter streamChat(ChatRequest request);
}