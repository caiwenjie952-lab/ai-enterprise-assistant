package com.wenjie.aiassistant.memory.impl;

import com.wenjie.aiassistant.memory.SpringAiMemoryTestService;
import lombok.RequiredArgsConstructor;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.client.advisor.MessageChatMemoryAdvisor;
import org.springframework.ai.chat.memory.ChatMemory;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class SpringAiMemoryTestServiceImpl implements SpringAiMemoryTestService {

    private final ChatClient chatClient;
    private final ChatMemory chatMemory;

    @Override
    public String chat(String conversationId, String message) {
        MessageChatMemoryAdvisor memoryAdvisor = MessageChatMemoryAdvisor.builder(chatMemory).build();

        return chatClient.prompt().user(message).advisors(advisor -> advisor.advisors(memoryAdvisor).
                param(ChatMemory.CONVERSATION_ID, conversationId)).call().content();
    }
}