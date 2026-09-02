package com.wenjie.aiassistant.service.impl;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.wenjie.aiassistant.client.ChatModelClient;
import com.wenjie.aiassistant.context.ChatContext;
import com.wenjie.aiassistant.context.ChatContextBuilder;
import com.wenjie.aiassistant.dto.ChatRequest;
import com.wenjie.aiassistant.lifecycle.ConversationLifecycleResult;
import com.wenjie.aiassistant.lifecycle.ConversationLifecycleService;
import com.wenjie.aiassistant.service.ChatStreamService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import java.io.IOException;
import java.util.Map;
import java.util.concurrent.CompletableFuture;

@Slf4j
@Service
@RequiredArgsConstructor
public class ChatStreamServiceImpl implements ChatStreamService {

    private final ChatModelClient chatModelClient;

    private final ChatContextBuilder chatContextBuilder;

    private final ConversationLifecycleService conversationLifecycleService;

    private final ObjectMapper objectMapper;

    @Override
    public SseEmitter streamChat(ChatRequest request) {
        SseEmitter emitter = new SseEmitter(0L);
        CompletableFuture.runAsync(() -> doStreamChat(request, emitter));
        return emitter;
    }

    private void doStreamChat(ChatRequest request, SseEmitter emitter) {
        long startTime = System.currentTimeMillis();
        ChatContext chatContext = chatContextBuilder.build(request);

        try {
            log.info("Received stream chat request, conversationId={}, contextMessages={}, recentMessages={}, hasSummary={}",
                    chatContext.getConversationId(),
                    chatContext.getContextMessages().size(),
                    chatContext.getRecentMessageCount(),
                    chatContext.getSummary() != null && !chatContext.getSummary().isBlank());

            sendEvent(emitter, "start", chatContext.getConversationId());

            String finalReply = chatModelClient.streamChat(chatContext.getConversationId(), chatContext.getContextMessages(),
                    chunk -> sendEvent(emitter, "message", chunk));

            ConversationLifecycleResult result = conversationLifecycleService.afterReply(chatContext, finalReply);

            sendEvent(emitter, "conversation", toConversationEventData(chatContext, result));
            sendEvent(emitter, "summary", result.getSummary() == null ? "" : result.getSummary());
            sendEvent(emitter, "done", chatContext.getConversationId());
            emitter.complete();

            long cost = System.currentTimeMillis() - startTime;
            log.info("Stream chat completed, conversationId={}, currentMessageIndex={}, summaryUpdated={}, cost={}ms",
                    chatContext.getConversationId(),
                    result.getCurrentMessageIndex(),
                    result.getSummaryUpdated(),
                    cost);
        } catch (Exception e) {
            log.error("Stream chat failed, conversationId={}, error={}",
                    chatContext.getConversationId(),
                    e.getMessage(),
                    e);

            try {
                sendEvent(emitter, "error", e.getMessage());
            } finally {
                emitter.complete();
            }
        }
    }

    private String toConversationEventData(ChatContext chatContext, ConversationLifecycleResult result) {
        try {
            return objectMapper.writeValueAsString(Map.of(
                    "conversationId", chatContext.getConversationId(),
                    "title", result.getTitle() == null ? "新会话" : result.getTitle()
            ));
        } catch (Exception e) {
            throw new RuntimeException("Conversation event json build failed", e);
        }
    }

    private void sendEvent(SseEmitter emitter, String eventName, String data) {
        try {
            emitter.send(SseEmitter.event()
                    .name(eventName)
                    .data(data == null ? "" : data));
        } catch (IOException e) {
            throw new RuntimeException("SSE message send failed", e);
        }
    }
}
