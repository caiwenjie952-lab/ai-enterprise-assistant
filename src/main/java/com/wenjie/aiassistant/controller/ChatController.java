package com.wenjie.aiassistant.controller;

import com.wenjie.aiassistant.common.Result;
import com.wenjie.aiassistant.config.AiProperties;
import com.wenjie.aiassistant.dto.ChatRequest;
import com.wenjie.aiassistant.dto.ChatResponse;
import com.wenjie.aiassistant.service.ChatService;
import com.wenjie.aiassistant.service.ChatStreamService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import java.util.Map;

@RestController
@RequestMapping("/chat")
@RequiredArgsConstructor
public class ChatController {

    private final ChatService chatService;

    private final ChatStreamService chatStreamService;

    private final AiProperties aiProperties;

    @GetMapping("/test")
    public Result<String> test() {
        return Result.success(chatService.test());
    }

    @GetMapping("/config")
    public Result<Map<String, Object>> config() {
        String apiKey = aiProperties.getApiKey();
        boolean apiKeyConfigured = apiKey != null
                && !apiKey.isBlank()
                && !apiKey.contains("${");

        return Result.success(Map.of(
                "provider", aiProperties.getProvider(),
                "model", aiProperties.getModel(),
                "baseUrl", aiProperties.getBaseUrl(),
                "apiKeyConfigured", apiKeyConfigured
        ));
    }

    @PostMapping
    public Result<ChatResponse> chat(@Valid @RequestBody ChatRequest request) {
        return Result.success(chatService.chat(request));
    }

    @PostMapping(value = "/stream", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    public SseEmitter streamChat(@Valid @RequestBody ChatRequest request) {
        return chatStreamService.streamChat(request);
    }
}
