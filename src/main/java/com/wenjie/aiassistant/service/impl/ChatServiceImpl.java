package com.wenjie.aiassistant.service.impl;

import com.wenjie.aiassistant.client.ChatModelClient;
import com.wenjie.aiassistant.config.AiProperties;
import com.wenjie.aiassistant.dto.ChatMessageDTO;
import com.wenjie.aiassistant.dto.ChatRequest;
import com.wenjie.aiassistant.dto.ChatResponse;
import com.wenjie.aiassistant.memory.ConversationMemoryService;
import com.wenjie.aiassistant.service.ChatService;
import com.wenjie.aiassistant.summary.ConversationSummaryService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

@Slf4j
@Service
@RequiredArgsConstructor
public class ChatServiceImpl implements ChatService {

    private final ChatModelClient chatModelClient;

    private final AiProperties aiProperties;

    private final ConversationMemoryService conversationMemoryService;

    private final ConversationSummaryService conversationSummaryService;

    @Override
    public String test() {
        return "AI assistant is running";
    }

    @Override
    public ChatResponse chat(ChatRequest request) {
        String conversationId = request.getConversationId();

        if (conversationId == null || conversationId.isBlank()) {
            conversationId = UUID.randomUUID().toString().replace("-", "");
        }

        String userMessage = request.getMessage();

        log.info("收到用户聊天请求，conversationId={}，message={}", conversationId, userMessage);

        long startTime = System.currentTimeMillis();

        try {
            int maxHistoryMessages = aiProperties.getMaxHistoryMessages() == null
                    ? 10
                    : aiProperties.getMaxHistoryMessages();

            int summaryTriggerMessages = aiProperties.getSummaryTriggerMessages() == null
                    ? 20
                    : aiProperties.getSummaryTriggerMessages();

            String summary = conversationMemoryService.getSummary(conversationId);

            List<ChatMessageDTO> recentMessages =
                    conversationMemoryService.getRecentMessages(conversationId, maxHistoryMessages);

            ChatMessageDTO currentUserMessage = new ChatMessageDTO("user", userMessage);

            List<ChatMessageDTO> contextMessages = new ArrayList<>();

            if (summary != null && !summary.isBlank()) {
                contextMessages.add(new ChatMessageDTO(
                        "system",
                        "以下是本次会话的长期摘要，请结合它理解用户上下文：" + summary
                ));
            }

            contextMessages.addAll(recentMessages);
            contextMessages.add(currentUserMessage);

            log.info("本次模型调用上下文消息数={}，最近历史数={}，summary是否存在={}",
                    contextMessages.size(), recentMessages.size(), summary != null && !summary.isBlank());

            String reply = chatModelClient.chat(contextMessages);

            conversationMemoryService.addMessages(conversationId, List.of(
                    currentUserMessage,
                    new ChatMessageDTO("assistant", reply)
            ));

            int totalMessages = conversationMemoryService.countMessages(conversationId);

            if (totalMessages >= summaryTriggerMessages) {
                List<ChatMessageDTO> allMessages = conversationMemoryService.getMessages(conversationId);
                String newSummary = conversationSummaryService.summarize(summary, allMessages);
                conversationMemoryService.updateSummary(conversationId, newSummary);
                summary = newSummary;

                log.info("会话摘要已更新，conversationId={}，totalMessages={}", conversationId, totalMessages);
            }

            long cost = System.currentTimeMillis() - startTime;

            log.info("聊天模型调用成功，conversationId={}，本次上下文消息数={}，总历史消息数={}，耗时={}ms",
                    conversationId, contextMessages.size(), totalMessages, cost);

            return new ChatResponse(
                    conversationId,
                    reply,
                    aiProperties.getProvider(),
                    aiProperties.getModel(),
                    summary
            );
        } catch (Exception e) {
            long cost = System.currentTimeMillis() - startTime;
            log.error("聊天模型调用失败，conversationId={}，耗时={}ms，错误={}",
                    conversationId, cost, e.getMessage(), e);
            throw e;
        }
    }
}