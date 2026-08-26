package com.wenjie.aiassistant.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@TableName("ai_chat_message")
public class AiChatMessageEntity {

    @TableId(type = IdType.AUTO)
    private Long id;

    private String conversationId;

    private Integer messageIndex;

    private String role;

    private String content;

    private LocalDateTime createTime;
}