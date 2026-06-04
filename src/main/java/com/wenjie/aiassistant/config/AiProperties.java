package com.wenjie.aiassistant.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

@Data
@Component
@ConfigurationProperties(prefix = "ai")
public class AiProperties {

    /**
     * 模型提供方，例如：mock、deepseek、qwen
     */
    private String provider;

    /**
     * 模型名称，例如：mock-chat、deepseek-chat、qwen-plus
     */
    private String model;
}