package com.wenjie.aiassistant.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

@Data
@Component
@ConfigurationProperties(prefix = "ai")
public class AiProperties {

    /**
     * 模型提供方：mock、deepseek、qwen
     */
    private String provider;

    /**
     * API 地址，例如：https://api.deepseek.com
     */
    private String baseUrl;

    /**
     * API Key，不要写死在代码里
     */
    private String apiKey;

    /**
     * 模型名称，例如：deepseek-v4-flash
     */
    private String model;

    /**
     * 系统提示词
     */
    private String systemPrompt;

    /**
     * 温度参数，控制回答随机性
     */
    private Double temperature;

    /**
     * 最大输出 token 数
     */
    private Integer maxTokens;

    /**
     * 每次调用模型时最多携带的历史消息条数
     */
    private Integer maxHistoryMessages;

    /**
     * 触发会话摘要的历史消息数量阈值
     */
    private Integer summaryTriggerMessages;
}