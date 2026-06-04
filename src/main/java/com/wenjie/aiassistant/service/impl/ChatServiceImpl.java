package com.wenjie.aiassistant.service.impl;

import com.wenjie.aiassistant.client.ChatModelClient;
import com.wenjie.aiassistant.service.ChatService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class ChatServiceImpl implements ChatService {

    private final ChatModelClient chatModelClient;

    @Override
    public String test() {
        return "AI assistant is running";
    }

    @Override
    public String chat(String message) {
        return chatModelClient.chat(message);
    }
}