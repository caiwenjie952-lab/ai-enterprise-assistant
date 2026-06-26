package com.wenjie.aiassistant.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class ConversationDetailResponse {

    /**
     * 会话 ID
     */
    private String conversationId;

    /**
     * 当前内存中的消息数量
     */
    private Integer memoryMessageCount;

    /**
     * 当前会话最新消息序号
     */
    private Integer currentMessageIndex;

    /**
     * 上一次生成摘要时的消息序号
     */
    private Integer lastSummaryMessageIndex;

    /**
     * 当前摘要
     */
    private String summary;

    /**
     * 当前内存中保留的消息
     */
    private List<ChatMessageDTO> messages;
}