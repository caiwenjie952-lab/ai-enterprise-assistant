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
    public String chat(List<ChatMessageDTO> messages) {
        ChatMessageDTO lastUserMessage = messages.stream()
                .filter(message -> "user".equals(message.getRole()))
                .reduce((first, second) -> second)
                .orElse(new ChatMessageDTO(0, "user", ""));

        return "当前模型提供方：" + aiProperties.getProvider()
                + "，模型名称：" + aiProperties.getModel()
                + "，历史消息数：" + messages.size()
                + "，收到你的问题：" + lastUserMessage.getContent();
    }

    @Override
    public String summarize(String oldSummary, List<ChatMessageDTO> messages) {
        return "Mock摘要：已有摘要=" + oldSummary
                + "，本次压缩消息数=" + messages.size();
    }

    @Override
    public String streamChat(List<ChatMessageDTO> messages, Consumer<String> chunkConsumer) {
        String reply = chat(messages);
        StringBuilder fullReply = new StringBuilder();

        for (int i = 0; i < reply.length(); i++) {
            String chunk = String.valueOf(reply.charAt(i));
            fullReply.append(chunk);
            chunkConsumer.accept(chunk);
        }

        return fullReply.toString();
    }

    @Override
    public String generateTitle(String userMessage) {
        if (userMessage == null || userMessage.isBlank()) {
            return "新会话";
        }

        String title = userMessage.trim()
                .replace("\r", "")
                .replace("\n", "");

        if (title.length() > 12) {
            title = title.substring(0, 12);
        }

        return title.isBlank() ? "新会话" : title;
    }
}