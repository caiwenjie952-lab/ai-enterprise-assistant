package com.wenjie.aiassistant.client.impl;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.wenjie.aiassistant.client.ChatModelClient;
import com.wenjie.aiassistant.config.AiProperties;
import com.wenjie.aiassistant.dto.ChatMessageDTO;
import com.wenjie.aiassistant.exception.BusinessException;
import lombok.Data;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;

@Slf4j
@Component
@RequiredArgsConstructor
@ConditionalOnProperty(prefix = "ai", name = "provider", havingValue = "deepseek")
public class DeepSeekChatModelClient implements ChatModelClient {

    private final AiProperties aiProperties;

    private final ObjectMapper objectMapper = new ObjectMapper();

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
            request.setStream(false);

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
                    || response.getChoices().getFirst().getMessage() == null
                    || response.getChoices().getFirst().getMessage().getContent() == null) {
                throw new BusinessException(502, "模型返回内容为空");
            }

            long cost = System.currentTimeMillis() - startTime;
            log.info("DeepSeek 模型调用完成，耗时={}ms", cost);

            return response.getChoices().getFirst().getMessage().getContent();

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
            request.setStream(false);

            DeepSeekChatResponse response = restClient.post()
                    .uri("/chat/completions")
                    .body(request)
                    .retrieve()
                    .body(DeepSeekChatResponse.class);

            if (response == null
                    || response.getChoices() == null
                    || response.getChoices().isEmpty()
                    || response.getChoices().getFirst().getMessage() == null
                    || response.getChoices().getFirst().getMessage().getContent() == null) {
                throw new BusinessException(502, "模型摘要返回内容为空");
            }

            long cost = System.currentTimeMillis() - startTime;
            log.info("DeepSeek 摘要生成完成，耗时={}ms", cost);

            return response.getChoices().getFirst().getMessage().getContent();

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

    @Override
    public String streamChat(List<ChatMessageDTO> messages, Consumer<String> chunkConsumer) {
        long startTime = System.currentTimeMillis();

        StringBuilder fullReply = new StringBuilder();

        try {
            log.info("开始调用 DeepSeek 原生流式接口，model={}，messages={}",
                    aiProperties.getModel(),
                    messages == null ? 0 : messages.size());

            RestClient restClient = RestClient.builder()
                    .baseUrl(aiProperties.getBaseUrl())
                    .defaultHeader(HttpHeaders.AUTHORIZATION, "Bearer " + aiProperties.getApiKey())
                    .defaultHeader(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE)
                    .build();

            List<DeepSeekMessage> deepSeekMessages = new ArrayList<>();

            if (aiProperties.getSystemPrompt() != null && !aiProperties.getSystemPrompt().isBlank()) {
                deepSeekMessages.add(new DeepSeekMessage("system", aiProperties.getSystemPrompt()));
            }

            if (messages != null) {
                for (ChatMessageDTO message : messages) {
                    deepSeekMessages.add(new DeepSeekMessage(message.getRole(), message.getContent()));
                }
            }

            DeepSeekChatRequest request = new DeepSeekChatRequest();
            request.setModel(aiProperties.getModel());
            request.setMessages(deepSeekMessages);
            request.setTemperature(aiProperties.getTemperature());
            request.setMaxTokens(aiProperties.getMaxTokens());
            request.setStream(true);

            restClient.post()
                    .uri("/chat/completions")
                    .accept(MediaType.TEXT_EVENT_STREAM)
                    .body(request)
                    .exchange((httpRequest, httpResponse) -> {
                        try (BufferedReader reader = new BufferedReader(
                                new InputStreamReader(httpResponse.getBody(), StandardCharsets.UTF_8))) {

                            String line;

                            while ((line = reader.readLine()) != null) {
                                if (line.isBlank()) {
                                    continue;
                                }

                                if (!line.startsWith("data:")) {
                                    continue;
                                }

                                String data = line.substring("data:".length()).trim();

                                if ("[DONE]".equals(data)) {
                                    break;
                                }

                                String content = parseStreamContent(data);

                                if (content != null && !content.isEmpty()) {
                                    fullReply.append(content);
                                    chunkConsumer.accept(content);
                                }
                            }

                            return null;
                        }
                    });

            long cost = System.currentTimeMillis() - startTime;

            log.info("DeepSeek 原生流式调用完成，回复长度={}，耗时={}ms",
                    fullReply.length(),
                    cost);

            return fullReply.toString();

        } catch (RestClientException e) {
            long cost = System.currentTimeMillis() - startTime;
            log.error("DeepSeek 原生流式调用失败，耗时={}ms，错误={}", cost, e.getMessage(), e);
            throw new BusinessException(502, "模型流式服务调用失败，请检查 API Key、模型名称或网络连接");
        } catch (Exception e) {
            long cost = System.currentTimeMillis() - startTime;
            log.error("DeepSeek 原生流式调用异常，耗时={}ms，错误={}", cost, e.getMessage(), e);
            throw new BusinessException(500, "模型流式调用异常，请稍后再试");
        }
    }

    private String parseStreamContent(String data) {
        try {
            JsonNode root = objectMapper.readTree(data);

            JsonNode choices = root.get("choices");

            if (choices == null || !choices.isArray() || choices.isEmpty()) {
                return "";
            }

            JsonNode delta = choices.get(0).get("delta");

            if (delta == null) {
                return "";
            }

            JsonNode content = delta.get("content");

            if (content == null || content.isNull()) {
                return "";
            }

            return content.asText();
        } catch (Exception e) {
            log.warn("解析 DeepSeek 流式响应失败，data={}", data, e);
            return "";
        }
    }

    @Data
    static class DeepSeekChatRequest {
        private String model;

        private List<DeepSeekMessage> messages;

        private Double temperature;

        @JsonProperty("max_tokens")
        private Integer maxTokens;

        private Boolean stream;
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
