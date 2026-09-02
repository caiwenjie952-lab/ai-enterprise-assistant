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
        String conversationId = chatContext.getConversationId();

        log.info("收到用户聊天请求，conversationId={}，message={}", conversationId, request.getMessage());

        try {
            // 1. 调用模型，拿到真实回复
            String reply = chatModelClient.chat(conversationId, chatContext.getContextMessages());
            // 2. 处理消息保存、标题、摘要、messageIndex、内存裁剪等生命周期逻辑
            ConversationLifecycleResult lifecycleResult = conversationLifecycleService.afterReply(chatContext, reply);

            long cost = System.currentTimeMillis() - startTime;

            log.info("聊天模型调用成功，conversationId={}，currentMessageIndex={}，summaryUpdated={}，耗时={}ms", conversationId, lifecycleResult.getCurrentMessageIndex(), lifecycleResult.getSummaryUpdated(), cost);

            // 3. 返回真实数据
            return new ChatResponse(conversationId, lifecycleResult.getTitle(), reply, aiProperties.getProvider(), aiProperties.getModel(), lifecycleResult.getSummary());

        } catch (Exception e) {
            long cost = System.currentTimeMillis() - startTime;

            log.error("聊天模型调用失败，conversationId={}，耗时={}ms，错误={}", conversationId, cost, e.getMessage(), e);

            throw e;
        }
    }
}