package com.wenjie.aiassistant.controller;

import com.wenjie.aiassistant.common.Result;
import com.wenjie.aiassistant.conversation.ConversationService;
import com.wenjie.aiassistant.dto.ConversationDetailResponse;
import com.wenjie.aiassistant.dto.ConversationListItemResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/conversation")
@RequiredArgsConstructor
public class ConversationController {

    private final ConversationService conversationService;

    @GetMapping("/list")
    public Result<List<ConversationListItemResponse>> list() {
        return Result.success(conversationService.listConversations());
    }

    @GetMapping("/{conversationId}")
    public Result<ConversationDetailResponse> detail(@PathVariable String conversationId) {

        return Result.success(conversationService.getConversationDetail(conversationId));
    }

    @DeleteMapping("/{conversationId}")
    public Result<Void> delete(@PathVariable String conversationId) {
        conversationService.deleteConversation(conversationId);
        return Result.success(null);
    }
}