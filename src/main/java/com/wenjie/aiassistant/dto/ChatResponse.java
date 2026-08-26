package com.wenjie.aiassistant.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class ChatResponse {

    /**
     * 会话 ID
     */
    private String conversationId;

    private String title;

    /**
     * 模型回复内容
     */
    private String reply;

    /**
     * 模型提供方
     */
    private String provider;

    /**
     * 模型名称
     */
    private String model;

    /**
     * 当前会话摘要
     */
    private String summary;


}