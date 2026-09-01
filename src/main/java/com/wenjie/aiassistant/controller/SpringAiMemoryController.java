package com.wenjie.aiassistant.controller;

import com.wenjie.aiassistant.common.Result;
import com.wenjie.aiassistant.memory.SpringAiMemoryTestService;
import lombok.Data;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/chat")
@RequiredArgsConstructor
public class SpringAiMemoryController {

    private final SpringAiMemoryTestService springAiMemoryTestService;

    @PostMapping("/spring-memory")
    public Result<String> chat(@RequestBody SpringMemoryChatRequest request) {
        String reply = springAiMemoryTestService.chat(request.getConversationId(), request.getMessage());

        return Result.success(reply);
    }

    @Data
    public static class SpringMemoryChatRequest {

        private String conversationId;

        private String message;
    }
}