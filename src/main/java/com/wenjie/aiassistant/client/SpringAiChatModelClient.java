package com.wenjie.aiassistant.client;

import com.wenjie.aiassistant.dto.ChatMessageDTO;
import lombok.RequiredArgsConstructor;
import org.springframework.ai.chat.messages.AssistantMessage;
import org.springframework.ai.chat.messages.Message;
import org.springframework.ai.chat.messages.SystemMessage;
import org.springframework.ai.chat.messages.UserMessage;
import org.springframework.ai.chat.model.ChatModel;
import org.springframework.ai.chat.prompt.Prompt;
import org.springframework.context.annotation.Primary;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Objects;
import java.util.function.Consumer;

@Component
@RequiredArgsConstructor
@Primary
public class SpringAiChatModelClient implements ChatModelClient {

    private final ChatModel chatModel;

    @Override
    public String chat(List<ChatMessageDTO> messages) {
        List<Message> springAiMessages = messages.stream().map(this::convertMessage).toList();

        Prompt prompt = new Prompt(springAiMessages);

        return Objects.requireNonNull(chatModel.call(prompt).getResult()).getOutput().getText();
    }

    @Override
    public String streamChat(List<ChatMessageDTO> messages, Consumer<String> chunkConsumer) {
        List<Message> springAiMessages = messages.stream().map(this::convertMessage).toList();

        Prompt prompt = new Prompt(springAiMessages);

        StringBuilder fullReply = new StringBuilder();

        chatModel.stream(prompt).doOnNext(response -> {
            String text = Objects.requireNonNull(response.getResult()).getOutput().getText();

            if (text != null && !text.isEmpty()) {
                fullReply.append(text);
                chunkConsumer.accept(text);
            }
        }).blockLast();

        return fullReply.toString();
    }

    @Override
    public String summarize(String oldSummary, List<ChatMessageDTO> messages) {
        StringBuilder content = new StringBuilder();

        if (oldSummary != null && !oldSummary.isBlank()) {
            content.append("已有摘要：\n").append(oldSummary).append("\n\n");
        }

        content.append("新增对话：\n");

        for (ChatMessageDTO message : messages) {
            content.append(message.getRole()).append(": ").append(message.getContent()).append("\n");
        }

        String prompt = """
                请根据已有摘要和新增对话生成新的会话摘要。
                
                要求：
                1. 保留重要背景、目标、偏好和关键结论
                2. 删除重复和无关内容
                3. 使用中文
                4. 控制在约500字以内
                5. 只输出摘要内容
                
                %s
                """.formatted(content);

        return chatModel.call(prompt);
    }

    @Override
    public String generateTitle(String userMessage) {
        String prompt = """
                请根据用户第一条消息生成一个简短的会话标题。
                
                要求：
                1. 使用中文
                2. 控制在5~15个字
                3. 不要加引号
                4. 不要解释
                5. 只返回标题
                
                用户消息：
                %s
                """.formatted(userMessage);

        return chatModel.call(prompt);
    }

    private Message convertMessage(ChatMessageDTO message) {
        return switch (message.getRole()) {
            case "system" -> new SystemMessage(message.getContent());
            case "assistant" -> new AssistantMessage(message.getContent());
            default -> new UserMessage(message.getContent());
        };
    }
}