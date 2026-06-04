package com.wenjie.aiassistant.client;

public interface ChatModelClient {

    /**
     * 调用聊天模型
     *
     * @param message 用户输入
     * @return 模型回复
     */
    String chat(String message);
}