package com.wenjie.aiassistant.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Data
public class ChatRequest {

    /**
     * 会话 ID。
     * 首次对话可以不传，后端自动生成。
     * 后续多轮对话需要带上同一个 conversationId。
     */
    private String conversationId;

    @NotBlank(message = "消息内容不能为空")
    private String message;
}