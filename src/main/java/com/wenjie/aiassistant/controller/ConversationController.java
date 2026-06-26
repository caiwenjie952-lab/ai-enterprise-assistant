package com.wenjie.aiassistant.controller;

import com.wenjie.aiassistant.common.Result;
import com.wenjie.aiassistant.dto.ChatMessageDTO;
import com.wenjie.aiassistant.dto.ConversationDetailResponse;
import com.wenjie.aiassistant.memory.ConversationMemoryService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/conversation")
@RequiredArgsConstructor
public class ConversationController {

    private final ConversationMemoryService conversationMemoryService;

    /**
     * 查询会话详情
     */
    @GetMapping("/{conversationId}")
    public Result<ConversationDetailResponse> getConversation(@PathVariable String conversationId) {
        List<ChatMessageDTO> messages = conversationMemoryService.getMessages(conversationId);

        ConversationDetailResponse response = new ConversationDetailResponse(
                conversationId,
                messages.size(),
                conversationMemoryService.getCurrentMessageIndex(conversationId),
                conversationMemoryService.getLastSummaryMessageIndex(conversationId),
                conversationMemoryService.getSummary(conversationId),
                messages
        );

        return Result.success(response);
    }

    /**
     * 清空会话记忆
     */
    @DeleteMapping("/{conversationId}")
    public Result<Boolean> clearConversation(@PathVariable String conversationId) {
        conversationMemoryService.clear(conversationId);
        return Result.success(true);
    }
}