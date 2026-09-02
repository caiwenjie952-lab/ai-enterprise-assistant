package com.wenjie.aiassistant.memory.impl;

import com.wenjie.aiassistant.dto.ChatMessageDTO;
import com.wenjie.aiassistant.memory.SpringAiChatMemoryRestoreService;
import com.wenjie.aiassistant.persistence.ConversationPersistenceService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.chat.memory.ChatMemory;
import org.springframework.ai.chat.messages.AssistantMessage;
import org.springframework.ai.chat.messages.Message;
import org.springframework.ai.chat.messages.UserMessage;
import org.springframework.stereotype.Service;

import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
public class SpringAiChatMemoryRestoreServiceImpl implements SpringAiChatMemoryRestoreService {

    private final ChatMemory chatMemory;
    private final ConversationPersistenceService conversationPersistenceService;

    @Override
    public void ensureLoaded(String conversationId) {
        if (conversationId == null || conversationId.isBlank()) {
            return;
        }

        if (!chatMemory.get(conversationId).isEmpty()) {
            return;
        }

        List<ChatMessageDTO> recentMessages = conversationPersistenceService.findRecentMessages(conversationId, 10);

        if (recentMessages.isEmpty()) {
            return;
        }

        List<Message> springAiMessages = recentMessages.stream().map(this::convertMessage).toList();

        chatMemory.add(conversationId, springAiMessages);

        log.info("Spring AI ChatMemory恢复完成，conversationId={}，messages={}", conversationId, springAiMessages.size());
    }

    private Message convertMessage(ChatMessageDTO message) {
        return switch (message.getRole()) {
            case "assistant" -> new AssistantMessage(message.getContent());
            default -> new UserMessage(message.getContent());
        };
    }
}