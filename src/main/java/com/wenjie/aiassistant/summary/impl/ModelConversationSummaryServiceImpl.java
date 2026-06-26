package com.wenjie.aiassistant.summary.impl;

import com.wenjie.aiassistant.client.ChatModelClient;
import com.wenjie.aiassistant.dto.ChatMessageDTO;
import com.wenjie.aiassistant.summary.ConversationSummaryService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
public class ModelConversationSummaryServiceImpl implements ConversationSummaryService {

    private final ChatModelClient chatModelClient;

    @Override
    public String summarize(String oldSummary, List<ChatMessageDTO> messages) {
        log.info("开始生成会话摘要，旧摘要是否存在={}，待摘要消息数={}",
                oldSummary != null && !oldSummary.isBlank(),
                messages == null ? 0 : messages.size());

        String summary = chatModelClient.summarize(oldSummary, messages);

        log.info("会话摘要生成完成，摘要长度={}", summary == null ? 0 : summary.length());

        return summary;
    }
}