package com.wenjie.aiassistant.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class ConversationListItemResponse {

    /**
     * 会话ID
     */
    private String conversationId;

    /**
     * 会话标题
     */
    private String title;

    /**
     * 当前内存中保留的消息数量
     */
    private Integer memoryMessageCount;

    /**
     * 当前最新消息序号
     */
    private Integer currentMessageIndex;

    /**
     * 上一次摘要更新到的消息序号
     */
    private Integer lastSummaryMessageIndex;

    /**
     * 是否已经存在摘要
     */
    private Boolean hasSummary;

    /**
     * 最后一条消息内容
     */
    private String lastMessage;

    /**
     * 会话创建时间
     */
    private Long createTime;

    /**
     * 会话最后更新时间
     */
    private Long updateTime;
}