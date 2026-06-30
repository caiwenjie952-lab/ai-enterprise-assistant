package com.wenjie.aiassistant.lifecycle.impl;

import com.wenjie.aiassistant.config.AiProperties;
import com.wenjie.aiassistant.context.ChatContext;
import com.wenjie.aiassistant.dto.ChatMessageDTO;
import com.wenjie.aiassistant.lifecycle.ConversationLifecycleResult;
import com.wenjie.aiassistant.lifecycle.ConversationLifecycleService;
import com.wenjie.aiassistant.memory.ConversationMemoryService;
import com.wenjie.aiassistant.summary.ConversationSummaryService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
public class ConversationLifecycleServiceImpl implements ConversationLifecycleService {

    private final AiProperties aiProperties;

    private final ConversationMemoryService conversationMemoryService;

    private final ConversationSummaryService conversationSummaryService;

    @Override
    public ConversationLifecycleResult afterReply(ChatContext chatContext, String assistantReply) {
        String conversationId = chatContext.getConversationId();

        int assistantMessageIndex = conversationMemoryService.nextMessageIndex(conversationId);
        ChatMessageDTO assistantMessage =
                new ChatMessageDTO(assistantMessageIndex, "assistant", assistantReply);

        conversationMemoryService.addMessages(conversationId, List.of(
                chatContext.getCurrentUserMessage(),
                assistantMessage
        ));

        int currentMessageIndex = conversationMemoryService.getCurrentMessageIndex(conversationId);
        int lastSummaryMessageIndex =
                conversationMemoryService.getLastSummaryMessageIndex(conversationId);

        String summary = chatContext.getSummary();
        boolean summaryUpdated = false;

        int summaryTriggerMessages = aiProperties.getSummaryTriggerMessages() == null
                ? 20
                : aiProperties.getSummaryTriggerMessages();

        int summaryIntervalMessages = aiProperties.getSummaryIntervalMessages() == null
                ? 10
                : aiProperties.getSummaryIntervalMessages();

        int summaryMaxMessages = aiProperties.getSummaryMaxMessages() == null
                ? 20
                : aiProperties.getSummaryMaxMessages();

        int maxMemoryMessages = aiProperties.getMaxMemoryMessages() == null
                ? 50
                : aiProperties.getMaxMemoryMessages();

        boolean needSummary = currentMessageIndex >= summaryTriggerMessages
                && currentMessageIndex - lastSummaryMessageIndex >= summaryIntervalMessages;

        if (needSummary) {
            List<ChatMessageDTO> summaryMessages =
                    conversationMemoryService.getMessagesAfterIndex(
                            conversationId,
                            lastSummaryMessageIndex,
                            summaryMaxMessages
                    );

            try {
                String newSummary = conversationSummaryService.summarize(summary, summaryMessages);

                conversationMemoryService.updateSummary(conversationId, newSummary);
                conversationMemoryService.updateLastSummaryMessageIndex(conversationId, currentMessageIndex);

                summary = newSummary;
                summaryUpdated = true;

                log.info("Conversation summary updated, conversationId={}, currentMessageIndex={}, lastSummaryMessageIndex={}, summaryMessages={}",
                        conversationId,
                        currentMessageIndex,
                        lastSummaryMessageIndex,
                        summaryMessages.size());
            } catch (Exception e) {
                log.warn("Conversation summary update failed; reply already generated. conversationId={}, currentMessageIndex={}, error={}",
                        conversationId,
                        currentMessageIndex,
                        e.getMessage(),
                        e);
            }
        } else {
            log.info("Conversation summary skipped, conversationId={}, currentMessageIndex={}, lastSummaryMessageIndex={}, summaryTriggerMessages={}, summaryIntervalMessages={}",
                    conversationId,
                    currentMessageIndex,
                    lastSummaryMessageIndex,
                    summaryTriggerMessages,
                    summaryIntervalMessages);
        }

        conversationMemoryService.trimMessages(conversationId, maxMemoryMessages);

        return new ConversationLifecycleResult(
                summary,
                currentMessageIndex,
                summaryUpdated
        );
    }
}
