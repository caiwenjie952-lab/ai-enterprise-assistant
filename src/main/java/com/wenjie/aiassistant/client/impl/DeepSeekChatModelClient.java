package com.wenjie.aiassistant.client.impl;

import com.wenjie.aiassistant.client.ChatModelClient;
import com.wenjie.aiassistant.config.AiProperties;
import com.wenjie.aiassistant.exception.BusinessException;
import lombok.Data;
import lombok.RequiredArgsConstructor;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;

import java.util.List;

@Component
@RequiredArgsConstructor
@ConditionalOnProperty(prefix = "ai", name = "provider", havingValue = "deepseek")
public class DeepSeekChatModelClient implements ChatModelClient {

    private final AiProperties aiProperties;

    @Override
    public String chat(String message) {
        try {
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
                    || response.getChoices().get(0).getMessage() == null
                    || response.getChoices().get(0).getMessage().getContent() == null) {
                throw new BusinessException(502, "模型返回内容为空");
            }

            return response.getChoices().get(0).getMessage().getContent();

        } catch (BusinessException e) {
            throw e;
        } catch (RestClientException e) {
            throw new BusinessException(502, "模型服务调用失败，请检查 API Key、模型名称或网络连接");
        } catch (Exception e) {
            throw new BusinessException(500, "模型调用异常，请稍后再试");
        }
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