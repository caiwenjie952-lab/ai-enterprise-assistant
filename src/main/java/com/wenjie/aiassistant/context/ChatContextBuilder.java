package com.wenjie.aiassistant.context;

import com.wenjie.aiassistant.config.AiProperties;
import com.wenjie.aiassistant.conversation.ConversationRestoreService;
import com.wenjie.aiassistant.dto.ChatMessageDTO;
import com.wenjie.aiassistant.dto.ChatRequest;
import com.wenjie.aiassistant.memory.ConversationMemoryService;
import com.wenjie.aiassistant.memory.SpringAiChatMemoryRestoreService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

@Component
@RequiredArgsConstructor
@Slf4j
public class ChatContextBuilder {

    private final AiProperties aiProperties;
    private final ConversationMemoryService conversationMemoryService;
    private final ConversationRestoreService conversationRestoreService;
    private final SpringAiChatMemoryRestoreService springAiChatMemoryRestoreService;

    public ChatContext build(ChatRequest request) {
        String conversationId = request.getConversationId();

        if (conversationId == null || conversationId.isBlank()) {
            conversationId = UUID.randomUUID().toString().replace("-", "");
        } else {
            conversationRestoreService.ensureLoaded(conversationId);
            springAiChatMemoryRestoreService.ensureLoaded(conversationId);
        }

        int maxHistoryMessages = aiProperties.getMaxHistoryMessages() == null ? 10 : aiProperties.getMaxHistoryMessages();

        String summary = conversationMemoryService.getSummary(conversationId);


        int userMessageIndex = conversationMemoryService.nextMessageIndex(conversationId);

        ChatMessageDTO currentUserMessage = new ChatMessageDTO(userMessageIndex, "user", request.getMessage());

        List<ChatMessageDTO> contextMessages = new ArrayList<>();

        if (summary != null && !summary.isBlank()) {
            contextMessages.add(new ChatMessageDTO(0, "system", "以下是本次会话的长期摘要，请结合它理解用户上下文：" + summary));
        }

        log.info("上下文构造完成，conversationId={}，summaryExists={}，userMessageIndex={}", conversationId, summary != null && !summary.isBlank(),userMessageIndex);

        return new ChatContext(conversationId, currentUserMessage, contextMessages, summary);
    }
}