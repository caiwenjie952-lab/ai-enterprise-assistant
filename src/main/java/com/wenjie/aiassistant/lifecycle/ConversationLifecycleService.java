package com.wenjie.aiassistant.lifecycle;

import com.wenjie.aiassistant.context.ChatContext;

public interface ConversationLifecycleService {

    ConversationLifecycleResult afterReply(ChatContext chatContext, String assistantReply);
}
