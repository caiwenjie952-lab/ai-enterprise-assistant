package com.wenjie.aiassistant.context;

import com.wenjie.aiassistant.config.AiProperties;
import com.wenjie.aiassistant.dto.ChatMessageDTO;
import com.wenjie.aiassistant.dto.ChatRequest;
import com.wenjie.aiassistant.memory.ConversationMemoryService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

@Component
@RequiredArgsConstructor
public class ChatContextBuilder {

    private final AiProperties aiProperties;

    private final ConversationMemoryService conversationMemoryService;

    public ChatContext build(ChatRequest request) {
        String conversationId = request.getConversationId();

        if (conversationId == null || conversationId.isBlank()) {
            conversationId = UUID.randomUUID().toString().replace("-", "");
        }

        int maxHistoryMessages = aiProperties.getMaxHistoryMessages() == null
                ? 10
                : aiProperties.getMaxHistoryMessages();

        String summary = conversationMemoryService.getSummary(conversationId);
        List<ChatMessageDTO> recentMessages =
                conversationMemoryService.getRecentMessages(conversationId, maxHistoryMessages);

        int userMessageIndex = conversationMemoryService.nextMessageIndex(conversationId);
        ChatMessageDTO currentUserMessage =
                new ChatMessageDTO(userMessageIndex, "user", request.getMessage());

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

        return new ChatContext(
                conversationId,
                currentUserMessage,
                contextMessages,
                summary,
                recentMessages.size()
        );
    }
}
