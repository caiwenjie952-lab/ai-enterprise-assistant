package com.wenjie.aiassistant.service.impl;

import com.wenjie.aiassistant.service.ChatService;
import org.springframework.stereotype.Service;

@Service
public class ChatServiceImpl implements ChatService {

    @Override
    public String test() {
        return "AI assistant is running";
    }
}