package com.wenjie.aiassistant.controller;

import com.wenjie.aiassistant.common.Result;
import com.wenjie.aiassistant.dto.ConversationDetailResponse;
import com.wenjie.aiassistant.dto.ConversationListItemResponse;
import com.wenjie.aiassistant.memory.ConversationMemoryService;
import com.wenjie.aiassistant.persistence.ConversationPersistenceService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/conversation")
@RequiredArgsConstructor
public class ConversationController {

    private final ConversationMemoryService conversationMemoryService;
    private final ConversationPersistenceService conversationPersistenceService;

    @GetMapping("/list")
    public Result<List<ConversationListItemResponse>> list() {
        return Result.success(conversationPersistenceService.listConversations());
    }

    @GetMapping("/{conversationId}")
    public Result<ConversationDetailResponse> detail(@PathVariable String conversationId) {
        ConversationDetailResponse detail =
                conversationPersistenceService.getConversationDetail(conversationId);

        if (detail == null) {
            return Result.fail(404, "会话不存在");
        }

        return Result.success(detail);
    }

    @DeleteMapping("/{conversationId}")
    public Result<Void> delete(@PathVariable String conversationId) {
        boolean deleted = conversationPersistenceService.deleteConversation(conversationId);

        if (!deleted) {
            return Result.fail(404, "会话不存在");
        }

        conversationMemoryService.clear(conversationId);

        return Result.success(null);
    }
}