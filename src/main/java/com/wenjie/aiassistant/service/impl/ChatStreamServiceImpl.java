package com.wenjie.aiassistant.service.impl;

import com.wenjie.aiassistant.client.ChatModelClient;
import com.wenjie.aiassistant.config.AiProperties;
import com.wenjie.aiassistant.dto.ChatMessageDTO;
import com.wenjie.aiassistant.dto.ChatRequest;
import com.wenjie.aiassistant.memory.ConversationMemoryService;
import com.wenjie.aiassistant.service.ChatStreamService;
import com.wenjie.aiassistant.summary.ConversationSummaryService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;

@Slf4j
@Service
@RequiredArgsConstructor
public class ChatStreamServiceImpl implements ChatStreamService {

    private final ChatModelClient chatModelClient;

    private final AiProperties aiProperties;

    private final ConversationMemoryService conversationMemoryService;

    private final ConversationSummaryService conversationSummaryService;

    @Override
    public SseEmitter streamChat(ChatRequest request) {
        // 0 表示不超时，也可以设置 60_000L
        SseEmitter emitter = new SseEmitter(0L);

        CompletableFuture.runAsync(() -> doStreamChat(request, emitter));

        return emitter;
    }

    private void doStreamChat(ChatRequest request, SseEmitter emitter) {
        String conversationId = request.getConversationId();

        if (conversationId == null || conversationId.isBlank()) {
            conversationId = UUID.randomUUID().toString().replace("-", "");
        }

        String userMessage = request.getMessage();

        log.info("收到流式聊天请求，conversationId={}，message={}", conversationId, userMessage);

        long startTime = System.currentTimeMillis();

        try {
            int maxHistoryMessages = aiProperties.getMaxHistoryMessages() == null
                    ? 10
                    : aiProperties.getMaxHistoryMessages();

            int summaryTriggerMessages = aiProperties.getSummaryTriggerMessages() == null
                    ? 20
                    : aiProperties.getSummaryTriggerMessages();

            int summaryIntervalMessages = aiProperties.getSummaryIntervalMessages() == null
                    ? 10
                    : aiProperties.getSummaryIntervalMessages();

            int summaryMaxMessages = aiProperties.getSummaryMaxMessages() == null
                    ? 20
                    : aiProperties.getSummaryMaxMessages();

            int maxMemoryMessages = aiProperties.getMaxMemoryMessages() == null
                    ? 50
                    : aiProperties.getMaxMemoryMessages();

            String summary = conversationMemoryService.getSummary(conversationId);

            List<ChatMessageDTO> recentMessages =
                    conversationMemoryService.getRecentMessages(conversationId, maxHistoryMessages);

            int userMessageIndex = conversationMemoryService.nextMessageIndex(conversationId);
            ChatMessageDTO currentUserMessage = new ChatMessageDTO(userMessageIndex, "user", userMessage);

            List<ChatMessageDTO> contextMessages = new ArrayList<>();

            if (summary != null && !summary.isBlank()) {
                contextMessages.add(new ChatMessageDTO(
                        0,
                        "system",
                        "以下是本次会话的长期摘要，请结合它理解用户上下文：" + summary
                ));
            }

            contextMessages.addAll(recentMessages);
            contextMessages.add(currentUserMessage);

            // 先发一个 start 事件，告诉前端 conversationId
            sendEvent(emitter, "start", conversationId);

            String finalReply = chatModelClient.streamChat(contextMessages, chunk -> sendEvent(emitter, "message", chunk));

            int assistantMessageIndex = conversationMemoryService.nextMessageIndex(conversationId);
            ChatMessageDTO assistantMessage = new ChatMessageDTO(assistantMessageIndex, "assistant", finalReply);

            conversationMemoryService.addMessages(conversationId, List.of(
                    currentUserMessage,
                    assistantMessage
            ));

            int currentMessageIndex = conversationMemoryService.getCurrentMessageIndex(conversationId);

            int lastSummaryMessageIndex =
                    conversationMemoryService.getLastSummaryMessageIndex(conversationId);

            boolean needSummary = currentMessageIndex >= summaryTriggerMessages
                    && currentMessageIndex - lastSummaryMessageIndex >= summaryIntervalMessages;

            if (needSummary) {
                List<ChatMessageDTO> summaryMessages =
                        conversationMemoryService.getMessagesAfterIndex(
                                conversationId,
                                lastSummaryMessageIndex,
                                summaryMaxMessages
                        );

                String newSummary = conversationSummaryService.summarize(summary, summaryMessages);

                conversationMemoryService.updateSummary(conversationId, newSummary);
                conversationMemoryService.updateLastSummaryMessageIndex(conversationId, currentMessageIndex);

                summary = newSummary;

                log.info("流式会话摘要已更新，conversationId={}，currentMessageIndex={}，lastSummaryMessageIndex={}，summaryMessages={}",
                        conversationId,
                        currentMessageIndex,
                        lastSummaryMessageIndex,
                        summaryMessages.size());
            }

            conversationMemoryService.trimMessages(conversationId, maxMemoryMessages);

            sendEvent(emitter, "summary", summary == null ? "" : summary);
            sendEvent(emitter, "done", conversationId);

            emitter.complete();

            long cost = System.currentTimeMillis() - startTime;

            log.info("流式聊天完成，conversationId={}，当前消息序号={}，耗时={}ms",
                    conversationId,
                    currentMessageIndex,
                    cost);

        } catch (Exception e) {
            log.error("流式聊天失败，conversationId={}，错误={}", conversationId, e.getMessage(), e);

            try {
                sendEvent(emitter, "error", e.getMessage());
            } finally {
                emitter.completeWithError(e);
            }
        }
    }

    private void sendEvent(SseEmitter emitter, String eventName, String data) {
        try {
            emitter.send(SseEmitter.event()
                    .name(eventName)
                    .data(data));
        } catch (IOException e) {
            throw new RuntimeException("SSE 消息发送失败", e);
        }
    }

}