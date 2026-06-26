package com.wenjie.aiassistant.client.impl;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.wenjie.aiassistant.client.ChatModelClient;
import com.wenjie.aiassistant.config.AiProperties;
import com.wenjie.aiassistant.dto.ChatMessageDTO;
import com.wenjie.aiassistant.exception.BusinessException;
import lombok.Data;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;

import java.util.ArrayList;
import java.util.List;

@Slf4j
@Component
@RequiredArgsConstructor
@ConditionalOnProperty(prefix = "ai", name = "provider", havingValue = "deepseek")
public class DeepSeekChatModelClient implements ChatModelClient {

    private final AiProperties aiProperties;

    @Override
    public String chat(List<ChatMessageDTO> messages) {
        long startTime = System.currentTimeMillis();

        try {
            log.info("开始调用 DeepSeek 模型，baseUrl={}，model={}，temperature={}，maxTokens={}，messages={}",
                    aiProperties.getBaseUrl(),
                    aiProperties.getModel(),
                    aiProperties.getTemperature(),
                    aiProperties.getMaxTokens(),
                    messages.size());

            RestClient restClient = RestClient.builder()
                    .baseUrl(aiProperties.getBaseUrl())
                    .defaultHeader("Authorization", "Bearer " + aiProperties.getApiKey())
                    .defaultHeader("Content-Type", MediaType.APPLICATION_JSON_VALUE)
                    .build();

            DeepSeekChatRequest request = new DeepSeekChatRequest();
            request.setModel(aiProperties.getModel());
            request.setTemperature(aiProperties.getTemperature());
            request.setMaxTokens(aiProperties.getMaxTokens());

            List<DeepSeekMessage> deepSeekMessages = new ArrayList<>();
            deepSeekMessages.add(new DeepSeekMessage("system", aiProperties.getSystemPrompt()));

            for (ChatMessageDTO message : messages) {
                deepSeekMessages.add(new DeepSeekMessage(message.getRole(), message.getContent()));
            }

            request.setMessages(deepSeekMessages);

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

            long cost = System.currentTimeMillis() - startTime;
            log.info("DeepSeek 模型调用完成，耗时={}ms", cost);

            return response.getChoices().get(0).getMessage().getContent();

        } catch (BusinessException e) {
            throw e;
        } catch (RestClientException e) {
            long cost = System.currentTimeMillis() - startTime;
            log.error("DeepSeek 模型服务调用失败，耗时={}ms，错误={}", cost, e.getMessage(), e);
            throw new BusinessException(502, "模型服务调用失败，请检查 API Key、模型名称或网络连接");
        } catch (Exception e) {
            long cost = System.currentTimeMillis() - startTime;
            log.error("DeepSeek 模型调用异常，耗时={}ms，错误={}", cost, e.getMessage(), e);
            throw new BusinessException(500, "模型调用异常，请稍后再试");
        }
    }

    @Override
    public String summarize(String oldSummary, List<ChatMessageDTO> messages) {
        long startTime = System.currentTimeMillis();

        try {
            log.info("开始调用 DeepSeek 生成摘要，model={}，messages={}",
                    aiProperties.getModel(),
                    messages == null ? 0 : messages.size());

            RestClient restClient = RestClient.builder()
                    .baseUrl(aiProperties.getBaseUrl())
                    .defaultHeader("Authorization", "Bearer " + aiProperties.getApiKey())
                    .defaultHeader("Content-Type", MediaType.APPLICATION_JSON_VALUE)
                    .build();

            StringBuilder conversationText = new StringBuilder();

            if (oldSummary != null && !oldSummary.isBlank()) {
                conversationText.append("【已有摘要】\n")
                        .append(oldSummary)
                        .append("\n\n");
            }

            conversationText.append("【新增会话内容】\n");

            if (messages != null) {
                for (ChatMessageDTO message : messages) {
                    String roleName = "user".equals(message.getRole()) ? "用户" : "助手";
                    conversationText.append(roleName)
                            .append("：")
                            .append(message.getContent())
                            .append("\n");
                }
            }

            DeepSeekChatRequest request = new DeepSeekChatRequest();
            request.setModel(aiProperties.getModel());
            request.setTemperature(0.3);
            request.setMaxTokens(800);
            request.setMessages(List.of(
                    new DeepSeekMessage("system", """
                        你是一个会话摘要助手。你的任务是把用户和助手的历史对话压缩成简洁、准确、可用于后续上下文理解的摘要。
                        要求：
                        1. 使用中文。
                        2. 控制在 500 字以内。
                        3. 保留用户目标、关键事实、已确认结论、待办事项。
                        4. 删除寒暄、重复内容和无关细节。
                        5. 不要编造原文没有的信息。
                        6. 直接输出摘要内容，不要输出解释。
                        """),
                    new DeepSeekMessage("user", conversationText.toString())
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
                throw new BusinessException(502, "模型摘要返回内容为空");
            }

            long cost = System.currentTimeMillis() - startTime;
            log.info("DeepSeek 摘要生成完成，耗时={}ms", cost);

            return response.getChoices().get(0).getMessage().getContent();

        } catch (BusinessException e) {
            throw e;
        } catch (RestClientException e) {
            long cost = System.currentTimeMillis() - startTime;
            log.error("DeepSeek 摘要服务调用失败，耗时={}ms，错误={}", cost, e.getMessage(), e);
            throw new BusinessException(502, "模型摘要服务调用失败，请检查 API Key、模型名称或网络连接");
        } catch (Exception e) {
            long cost = System.currentTimeMillis() - startTime;
            log.error("DeepSeek 摘要生成异常，耗时={}ms，错误={}", cost, e.getMessage(), e);
            throw new BusinessException(500, "模型摘要生成异常，请稍后再试");
        }
    }

    @Data
    static class DeepSeekChatRequest {
        private String model;

        private List<DeepSeekMessage> messages;

        private Double temperature;

        @JsonProperty("max_tokens")
        private Integer maxTokens;
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