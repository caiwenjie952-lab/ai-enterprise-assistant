package com.wenjie.aiassistant.service.impl;

import com.wenjie.aiassistant.client.ChatModelClient;
import com.wenjie.aiassistant.service.ChatService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

@Slf4j
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
        log.info("收到用户聊天请求，message={}", message);

        long startTime = System.currentTimeMillis();

        try {
            String reply = chatModelClient.chat(message);
            long cost = System.currentTimeMillis() - startTime;
            log.info("聊天模型调用成功，耗时={}ms", cost);
            return reply;
        } catch (Exception e) {
            long cost = System.currentTimeMillis() - startTime;
            log.error("聊天模型调用失败，耗时={}ms，错误={}", cost, e.getMessage(), e);
            throw e;
        }
    }
}