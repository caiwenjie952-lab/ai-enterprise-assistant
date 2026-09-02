package com.wenjie.aiassistant.client;

import com.wenjie.aiassistant.dto.ChatMessageDTO;
import com.wenjie.aiassistant.prompt.AiPromptTemplateRenderer;
import lombok.RequiredArgsConstructor;
import lombok.extern.log4j.Log4j2;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.client.advisor.MessageChatMemoryAdvisor;
import org.springframework.ai.chat.memory.ChatMemory;
import org.springframework.ai.chat.messages.AssistantMessage;
import org.springframework.ai.chat.messages.Message;
import org.springframework.ai.chat.messages.SystemMessage;
import org.springframework.ai.chat.messages.UserMessage;
import org.springframework.context.annotation.Primary;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.function.Consumer;

@Component
@RequiredArgsConstructor
@Primary
@Log4j2
public class SpringAiChatModelClient implements ChatModelClient {

    private final ChatClient chatClient;

    private final AiPromptTemplateRenderer promptTemplateRenderer;

    private final MessageChatMemoryAdvisor messageChatMemoryAdvisor;

    @Override
    public String chat(String conversationId, List<ChatMessageDTO> messages) {
        List<Message> springAiMessages = messages.stream().map(this::convertMessage).toList();

        return chatClient.prompt().messages(springAiMessages).advisors(advisor -> advisor.advisors(messageChatMemoryAdvisor).param(ChatMemory.CONVERSATION_ID, conversationId)).call().content();
    }

    @Override
    public String streamChat(String conversationId, List<ChatMessageDTO> messages, Consumer<String> chunkConsumer) {
        List<Message> springAiMessages = messages.stream().map(this::convertMessage).toList();

        StringBuilder fullReply = new StringBuilder();

        chatClient.prompt().messages(springAiMessages).advisors(advisor -> advisor.advisors(messageChatMemoryAdvisor).param(ChatMemory.CONVERSATION_ID, conversationId)).stream().content().doOnNext(chunk -> {
            if (chunk != null && !chunk.isEmpty()) {
                fullReply.append(chunk);
                chunkConsumer.accept(chunk);
            }
        }).blockLast();

        return fullReply.toString();
    }

    @Override
    public String summarize(String oldSummary, List<ChatMessageDTO> messages) {
        String prompt = promptTemplateRenderer.renderSummaryPrompt(oldSummary, messages);


        return chatClient.prompt().user(prompt).call().content();
    }

    @Override
    public String generateTitle(String userMessage) {
        log.info("开始生成会话标题，userMessage={}", userMessage);
        String prompt = promptTemplateRenderer.renderTitlePrompt(userMessage);

        return chatClient.prompt().user(prompt).call().content();
    }

    private Message convertMessage(ChatMessageDTO message) {
        return switch (message.getRole()) {
            case "system" -> new SystemMessage(message.getContent());
            case "assistant" -> new AssistantMessage(message.getContent());
            default -> new UserMessage(message.getContent());
        };
    }
}