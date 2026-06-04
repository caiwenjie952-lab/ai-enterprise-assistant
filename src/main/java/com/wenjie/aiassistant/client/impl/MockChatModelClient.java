package com.wenjie.aiassistant.client.impl;

import com.wenjie.aiassistant.client.ChatModelClient;
import com.wenjie.aiassistant.config.AiProperties;
import lombok.RequiredArgsConstructor;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
@ConditionalOnProperty(prefix = "ai", name = "provider", havingValue = "mock", matchIfMissing = true)
public class MockChatModelClient implements ChatModelClient {

    private final AiProperties aiProperties;

    @Override
    public String chat(String message) {
        return "当前模型提供方：" + aiProperties.getProvider()
                + "，模型名称：" + aiProperties.getModel()
                + "，收到你的问题：" + message;
    }
}