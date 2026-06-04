package com.wenjie.aiassistant.client.impl;

import com.wenjie.aiassistant.client.ChatModelClient;
import com.wenjie.aiassistant.config.AiProperties;
import lombok.Data;
import lombok.RequiredArgsConstructor;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;

import java.util.List;

@Component
@RequiredArgsConstructor
@ConditionalOnProperty(prefix = "ai", name = "provider", havingValue = "deepseek")
public class DeepSeekChatModelClient implements ChatModelClient {

    private final AiProperties aiProperties;

    @Override
    public String chat(String message) {
        RestClient restClient = RestClient.builder()
                .baseUrl(aiProperties.getBaseUrl())
                .defaultHeader("Authorization", "Bearer " + aiProperties.getApiKey())
                .defaultHeader("Content-Type", MediaType.APPLICATION_JSON_VALUE)
                .build();

        DeepSeekChatRequest request = new DeepSeekChatRequest();
        request.setModel(aiProperties.getModel());
        request.setMessages(List.of(
                new DeepSeekMessage("system", "你是一个专业、简洁、可靠的企业 AI 助手。"),
                new DeepSeekMessage("user", message)
        ));

        DeepSeekChatResponse response = restClient.post()
                .uri("/chat/completions")
                .body(request)
                .retrieve()
                .body(DeepSeekChatResponse.class);

        if (response == null
                || response.getChoices() == null
                || response.getChoices().isEmpty()
                || response.getChoices().getFirst().getMessage() == null) {
            return "模型暂无回复";
        }

        return response.getChoices().getFirst().getMessage().getContent();
    }

    @Data
    static class DeepSeekChatRequest {
        private String model;
        private List<DeepSeekMessage> messages;
    }

    @Data
    @RequiredArgsConstructor
    static class DeepSeekMessage {
        private final String role;
        private final String content;
    }

    @Data
    static class DeepSeekChatResponse {
        private List<DeepSeekChoice> choices;
    }

    @Data
    static class DeepSeekChoice {
        private DeepSeekMessageResult message;
    }

    @Data
    static class DeepSeekMessageResult {
        private String role;
        private String content;
    }
}