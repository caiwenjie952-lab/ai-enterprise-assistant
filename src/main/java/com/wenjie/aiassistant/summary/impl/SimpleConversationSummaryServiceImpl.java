package com.wenjie.aiassistant.summary.impl;

import com.wenjie.aiassistant.dto.ChatMessageDTO;
import com.wenjie.aiassistant.summary.ConversationSummaryService;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.stream.Collectors;

@Service
public class SimpleConversationSummaryServiceImpl implements ConversationSummaryService {

    @Override
    public String summarize(String oldSummary, List<ChatMessageDTO> messages) {
        String newContent = messages.stream()
                .map(message -> {
                    String role = "user".equals(message.getRole()) ? "用户" : "助手";
                    return role + "：" + message.getContent();
                })
                .collect(Collectors.joining("；"));

        if (oldSummary == null || oldSummary.isBlank()) {
            return "会话摘要：" + newContent;
        }

        return oldSummary + "；" + newContent;
    }
}