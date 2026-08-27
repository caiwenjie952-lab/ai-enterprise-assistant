package com.wenjie.aiassistant.conversation;

import com.wenjie.aiassistant.dto.ConversationDetailResponse;
import com.wenjie.aiassistant.dto.ConversationListItemResponse;

import java.util.List;

public interface ConversationService {

    List<ConversationListItemResponse> listConversations();

    ConversationDetailResponse getConversationDetail(String conversationId);

    void deleteConversation(String conversationId);
}