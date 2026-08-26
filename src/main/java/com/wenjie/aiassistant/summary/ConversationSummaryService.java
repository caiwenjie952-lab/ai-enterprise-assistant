package com.wenjie.aiassistant.summary;

import com.wenjie.aiassistant.dto.ChatMessageDTO;

import java.util.List;

public interface ConversationSummaryService {

    /**
     * 生成会话摘要
     *
     * @param oldSummary 旧摘要
     * @param messages   需要纳入摘要的消息
     * @return 新摘要
     */
    String summarize(String oldSummary, List<ChatMessageDTO> messages);
}