    package com.wenjie.aiassistant.client;

    import com.wenjie.aiassistant.dto.ChatMessageDTO;

    import java.util.List;
    import java.util.function.Consumer;

    public interface ChatModelClient {

        /**
         * 调用聊天模型
         */
        String chat(List<ChatMessageDTO> messages);

        /**
         * 调用模型生成会话摘要
         */
        String summarize(String oldSummary, List<ChatMessageDTO> messages);

        /**
         * 原生流式聊天
         *
         * @param messages 上下文消息
         * @param chunkConsumer 每收到一个模型输出片段，就回调一次
         * @return 完整回复内容
         */
        String streamChat(List<ChatMessageDTO> messages, Consumer<String> chunkConsumer);
    }