package com.wenjie.aiassistant.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@TableName("ai_conversation")
public class AiConversationEntity {

    @TableId(type = IdType.AUTO)
    private Long id;

    private String conversationId;

    private String title;

    private String summary;

    private Integer currentMessageIndex;

    private Integer lastSummaryMessageIndex;

    private LocalDateTime createTime;

    private LocalDateTime updateTime;
}