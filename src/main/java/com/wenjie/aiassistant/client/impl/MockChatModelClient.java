package com.wenjie.aiassistant.client.impl;

import com.wenjie.aiassistant.client.ChatModelClient;
import com.wenjie.aiassistant.config.AiProperties;
import com.wenjie.aiassistant.dto.ChatMessageDTO;
import lombok.RequiredArgsConstructor;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.function.Consumer;

@Component
@RequiredArgsConstructor
@ConditionalOnProperty(prefix = "ai", name = "provider", havingValue = "mock", matchIfMissing = true)
public class MockChatModelClient implements ChatModelClient {

    private final AiProperties aiProperties;

    @Override
    public String chat(String conversationId, List<ChatMessageDTO> messages) {
        ChatMessageDTO lastUserMessage = messages.stream().filter(message -> "user".equals(message.getRole())).reduce((first, second) -> second).orElse(new ChatMessageDTO(null, "user", ""));

        return "当前模型提供方：" + aiProperties.getProvider() + "，模型名称：" + aiProperties.getModel() + "，历史消息数：" + messages.size() + "，收到你的问题：" + lastUserMessage.getContent();
    }


    @Override
    public String summarize(String oldSummary, List<ChatMessageDTO> messages) {
        return "";
    }

    @Override
    public String streamChat(String conversationId, List<ChatMessageDTO> messages, Consumer<String> chunkConsumer) {
        return "";
    }


    @Override
    public String generateTitle(String userMessage) {
        return "";
    }
}
