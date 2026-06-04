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
}