package com.wenjie.aiassistant.lifecycle.impl;

import com.wenjie.aiassistant.client.ChatModelClient;
import com.wenjie.aiassistant.config.AiProperties;
import com.wenjie.aiassistant.context.ChatContext;
import com.wenjie.aiassistant.dto.ChatMessageDTO;
import com.wenjie.aiassistant.lifecycle.ConversationLifecycleResult;
import com.wenjie.aiassistant.lifecycle.ConversationLifecycleService;
import com.wenjie.aiassistant.memory.ConversationMemoryService;
import com.wenjie.aiassistant.persistence.ConversationPersistenceService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
public class ConversationLifecycleServiceImpl implements ConversationLifecycleService {

    private final AiProperties aiProperties;
    private final ChatModelClient chatModelClient;
    private final ConversationMemoryService conversationMemoryService;
    private final ConversationPersistenceService conversationPersistenceService;

    @Override
    public ConversationLifecycleResult afterReply(ChatContext chatContext, String reply) {
        String conversationId = chatContext.getConversationId();

        // 1. 生成 assistant 消息序号
        int assistantMessageIndex = conversationMemoryService.nextMessageIndex(conversationId);

        ChatMessageDTO assistantMessage = new ChatMessageDTO(assistantMessageIndex, "assistant", reply);

        ChatMessageDTO userMessage = chatContext.getCurrentUserMessage();

        List<ChatMessageDTO> newMessages = List.of(userMessage, assistantMessage);


        // 3. 确保数据库中存在会话主记录
        conversationPersistenceService.ensureConversation(conversationId);

        // 4. 保存本轮 user + assistant 消息
        conversationPersistenceService.saveMessages(conversationId, newMessages);

        // 5. 更新当前 messageIndex
        int currentMessageIndex = conversationMemoryService.getCurrentMessageIndex(conversationId);

        conversationPersistenceService.updateCurrentMessageIndex(conversationId, currentMessageIndex);

        // 6. 新会话标题
        String title = conversationMemoryService.getTitle(conversationId);

        if (title == null || title.isBlank()) {
            title = generateTitle(userMessage.getContent());

            if (title != null && !title.isBlank()) {
                conversationMemoryService.updateTitle(conversationId, title);
                conversationPersistenceService.updateTitle(conversationId, title);
            }
        }

        // 7. 判断是否需要更新摘要
        boolean summaryUpdated = false;

        String summary = conversationMemoryService.getSummary(conversationId);
        int lastSummaryMessageIndex = conversationMemoryService.getLastSummaryMessageIndex(conversationId);

        Integer summaryTriggerMessages = aiProperties.getSummaryTriggerMessages();
        Integer summaryIntervalMessages = aiProperties.getSummaryIntervalMessages();
        Integer summaryMaxMessages = aiProperties.getSummaryMaxMessages();

        int trigger = summaryTriggerMessages == null ? 30 : summaryTriggerMessages;
        int interval = summaryIntervalMessages == null ? 10 : summaryIntervalMessages;
        int maxSummaryMessages = summaryMaxMessages == null ? 12 : summaryMaxMessages;

        boolean reachedTrigger = currentMessageIndex >= trigger;

        boolean reachedInterval = currentMessageIndex - lastSummaryMessageIndex >= interval;

        if (reachedTrigger && reachedInterval) {
            List<ChatMessageDTO> messagesToSummarize = conversationPersistenceService.findMessagesAfterIndex(
                    conversationId, lastSummaryMessageIndex, maxSummaryMessages);
            if (!messagesToSummarize.isEmpty()) {
                String newSummary = chatModelClient.summarize(summary, messagesToSummarize);

                if (newSummary != null && !newSummary.isBlank()) {
                    int newLastSummaryMessageIndex = messagesToSummarize.get(messagesToSummarize.size() - 1).getMessageIndex();

                    conversationMemoryService.updateSummary(conversationId, newSummary);

                    conversationMemoryService.updateLastSummaryMessageIndex(conversationId, newLastSummaryMessageIndex);

                    conversationPersistenceService.updateSummary(conversationId, newSummary, newLastSummaryMessageIndex);

                    summary = newSummary;
                    lastSummaryMessageIndex = newLastSummaryMessageIndex;
                    summaryUpdated = true;
                }
            }
        }

        // 9. 更新时间
        conversationMemoryService.touchConversation(conversationId);

        log.info("会话生命周期处理完成，conversationId={}，currentMessageIndex={}，lastSummaryMessageIndex={}，summaryUpdated={}", conversationId, currentMessageIndex, lastSummaryMessageIndex, summaryUpdated);

        return new ConversationLifecycleResult(summary, currentMessageIndex, summaryUpdated, title, title != null && !title.isBlank());
    }

    private String generateTitle(String userMessage) {
        try {
            return chatModelClient.generateTitle(userMessage);
        } catch (Exception e) {
            log.warn("生成会话标题失败，message={}，error={}", userMessage, e.getMessage());
            return "";
        }
    }
}