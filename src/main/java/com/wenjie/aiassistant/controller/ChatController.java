package com.wenjie.aiassistant.controller;

import com.wenjie.aiassistant.common.Result;
import com.wenjie.aiassistant.dto.ChatRequest;
import com.wenjie.aiassistant.dto.ChatResponse;
import com.wenjie.aiassistant.service.ChatService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

@RestController
@RequiredArgsConstructor
@RequestMapping("/chat")
public class ChatController {

    private final ChatService chatService;

    @GetMapping("/test")
    public Result<String> test() {
        return Result.success(chatService.test());
    }

    @PostMapping
    public Result<ChatResponse> chat(@Valid @RequestBody ChatRequest request) {
        return Result.success(chatService.chat(request));
    }
}