package com.wenjie.aiassistant.config;

import com.wenjie.aiassistant.advisor.AiCallTimingAdvisor;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.client.advisor.MessageChatMemoryAdvisor;
import org.springframework.ai.chat.client.advisor.SimpleLoggerAdvisor;
import org.springframework.ai.chat.memory.ChatMemory;
import org.springframework.ai.chat.memory.MessageWindowChatMemory;
import org.springframework.ai.chat.model.ChatModel;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class SpringAiConfig {


    @Bean
    public ChatClient chatClient(ChatModel chatModel, AiCallTimingAdvisor aiCallTimingAdvisor) {
        return ChatClient.builder(chatModel).defaultAdvisors(aiCallTimingAdvisor, new SimpleLoggerAdvisor()).build();
    }

    @Bean
    public ChatMemory chatMemory() {
        return MessageWindowChatMemory.builder().maxMessages(10).build();
    }

    //会自动维持有限的消息窗口；底层 ChatMemoryRepository 决定消息实际存在哪里
    @Bean
    public MessageChatMemoryAdvisor messageChatMemoryAdvisor(ChatMemory chatMemory) {
        return MessageChatMemoryAdvisor.builder(chatMemory).build();
    }
}