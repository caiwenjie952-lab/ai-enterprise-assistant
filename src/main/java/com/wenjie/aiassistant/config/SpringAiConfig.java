package com.wenjie.aiassistant.config;

import com.wenjie.aiassistant.advisor.AiCallTimingAdvisor;
import org.springframework.ai.chat.client.ChatClient;
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
}