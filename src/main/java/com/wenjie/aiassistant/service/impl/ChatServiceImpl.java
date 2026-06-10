package com.wenjie.aiassistant.service.impl;

import com.wenjie.aiassistant.client.ChatModelClient;
import com.wenjie.aiassistant.config.AiProperties;
import com.wenjie.aiassistant.dto.ChatMessageDTO;
import com.wenjie.aiassistant.dto.ChatRequest;
import com.wenjie.aiassistant.dto.ChatResponse;
import com.wenjie.aiassistant.memory.ConversationMemoryService;
import com.wenjie.aiassistant.service.ChatService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.UUID;

@Slf4j
@Service
@RequiredArgsConstructor
public class ChatServiceImpl implements ChatService {

    private final ChatModelClient chatModelClient;

    private final AiProperties aiProperties;

    private final ConversationMemoryService conversationMemoryService;

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
            List<ChatMessageDTO> historyMessages = conversationMemoryService.getMessages(conversationId);

            ChatMessageDTO currentUserMessage = new ChatMessageDTO("user", userMessage);
            historyMessages.add(currentUserMessage);

            String reply = chatModelClient.chat(historyMessages);

            conversationMemoryService.addMessages(conversationId, List.of(
                    currentUserMessage,
                    new ChatMessageDTO("assistant", reply)
            ));

            long cost = System.currentTimeMillis() - startTime;

            log.info("聊天模型调用成功，conversationId={}，历史消息数={}，耗时={}ms",
                    conversationId, historyMessages.size(), cost);

            return new ChatResponse(
                    conversationId,
                    reply,
                    aiProperties.getProvider(),
                    aiProperties.getModel()
            );
        } catch (Exception e) {
            long cost = System.currentTimeMillis() - startTime;
            log.error("聊天模型调用失败，conversationId={}，耗时={}ms，错误={}",
                    conversationId, cost, e.getMessage(), e);
            throw e;
        }
    }
}