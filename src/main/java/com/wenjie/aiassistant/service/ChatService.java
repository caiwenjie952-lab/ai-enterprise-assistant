package com.wenjie.aiassistant.service;

import com.wenjie.aiassistant.dto.ChatRequest;
import com.wenjie.aiassistant.dto.ChatResponse;

public interface ChatService {

    String test();

    ChatResponse chat(ChatRequest request);
}