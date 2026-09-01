package com.wenjie.aiassistant.prompt;

import com.wenjie.aiassistant.dto.ChatMessageDTO;
import org.springframework.ai.chat.prompt.PromptTemplate;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.Resource;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Map;

@Component
public class AiPromptTemplateRenderer {

    private final Resource titlePromptResource;
    private final Resource summaryPromptResource;

    public AiPromptTemplateRenderer(@Value("classpath:/prompts/title.st") Resource titlePromptResource,
                                    @Value("classpath:/prompts/summary.st") Resource summaryPromptResource) {
        this.titlePromptResource = titlePromptResource;
        this.summaryPromptResource = summaryPromptResource;
    }

    public String renderTitlePrompt(String userMessage) {
        PromptTemplate promptTemplate = new PromptTemplate(titlePromptResource);

        return promptTemplate.render(Map.of("userMessage", userMessage));
    }

    public String renderSummaryPrompt(String oldSummary, List<ChatMessageDTO> messages) {
        PromptTemplate promptTemplate = new PromptTemplate(summaryPromptResource);

        return promptTemplate.render(Map.of("oldSummary", oldSummary == null ? "" : oldSummary, "newMessages", formatMessages(messages)));
    }

    private String formatMessages(List<ChatMessageDTO> messages) {
        StringBuilder content = new StringBuilder();

        for (ChatMessageDTO message : messages) {
            content.append(message.getRole()).append(": ").append(message.getContent()).append("\n");
        }

        return content.toString();
    }
}