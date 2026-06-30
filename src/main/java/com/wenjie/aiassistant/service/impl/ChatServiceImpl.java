package com.wenjie.aiassistant.service.impl;

import com.wenjie.aiassistant.client.ChatModelClient;
import com.wenjie.aiassistant.config.AiProperties;
import com.wenjie.aiassistant.context.ChatContext;
import com.wenjie.aiassistant.context.ChatContextBuilder;
import com.wenjie.aiassistant.dto.ChatRequest;
import com.wenjie.aiassistant.dto.ChatResponse;
import com.wenjie.aiassistant.lifecycle.ConversationLifecycleResult;
import com.wenjie.aiassistant.lifecycle.ConversationLifecycleService;
import com.wenjie.aiassistant.service.ChatService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

@Slf4j
@Service
@RequiredArgsConstructor
public class ChatServiceImpl implements ChatService {

    private final ChatModelClient chatModelClient;

    private final AiProperties aiProperties;

    private final ChatContextBuilder chatContextBuilder;

    private final ConversationLifecycleService conversationLifecycleService;

    @Override
    public String test() {
        return "AI assistant is running";
    }

    @Override
    public ChatResponse chat(ChatRequest request) {
        long startTime = System.currentTimeMillis();
        ChatContext chatContext = chatContextBuilder.build(request);

        try {
            log.info("Received chat request, conversationId={}, contextMessages={}, recentMessages={}, hasSummary={}",
                    chatContext.getConversationId(),
                    chatContext.getContextMessages().size(),
                    chatContext.getRecentMessageCount(),
                    chatContext.getSummary() != null && !chatContext.getSummary().isBlank());

            String reply = chatModelClient.chat(chatContext.getContextMessages());
            ConversationLifecycleResult result = conversationLifecycleService.afterReply(chatContext, reply);

            long cost = System.currentTimeMillis() - startTime;
            log.info("Chat completed, conversationId={}, currentMessageIndex={}, summaryUpdated={}, cost={}ms",
                    chatContext.getConversationId(),
                    result.getCurrentMessageIndex(),
                    result.getSummaryUpdated(),
                    cost);

            return new ChatResponse(
                    chatContext.getConversationId(),
                    reply,
                    aiProperties.getProvider(),
                    aiProperties.getModel(),
                    result.getSummary()
            );
        } catch (Exception e) {
            long cost = System.currentTimeMillis() - startTime;
            log.error("Chat failed, conversationId={}, cost={}ms, error={}",
                    chatContext.getConversationId(),
                    cost,
                    e.getMessage(),
                    e);
            throw e;
        }
    }
}
