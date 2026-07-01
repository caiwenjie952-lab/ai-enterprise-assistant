package com.wenjie.aiassistant.memory.impl;

import com.wenjie.aiassistant.dto.ChatMessageDTO;
import com.wenjie.aiassistant.memory.ConversationMemoryService;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;

@Service
public class InMemoryConversationMemoryServiceImpl implements ConversationMemoryService {

    private final ConcurrentHashMap<String, List<ChatMessageDTO>> memory = new ConcurrentHashMap<>();

    private final ConcurrentHashMap<String, String> summaryMemory = new ConcurrentHashMap<>();

    private final ConcurrentHashMap<String, String> titleMemory = new ConcurrentHashMap<>();

    private final ConcurrentHashMap<String, Integer> lastSummaryMessageIndexMemory = new ConcurrentHashMap<>();

    private final ConcurrentHashMap<String, AtomicInteger> messageIndexMemory = new ConcurrentHashMap<>();

    @Override
    public List<ChatMessageDTO> getMessages(String conversationId) {
        return new ArrayList<>(memory.getOrDefault(conversationId, new ArrayList<>()));
    }

    @Override
    public List<ChatMessageDTO> getRecentMessages(String conversationId, int limit) {
        List<ChatMessageDTO> messages = memory.getOrDefault(conversationId, new ArrayList<>());

        if (limit <= 0 || messages.isEmpty()) {
            return new ArrayList<>();
        }

        List<ChatMessageDTO> sortedMessages = messages.stream()
                .sorted(Comparator.comparing(ChatMessageDTO::getMessageIndex))
                .toList();

        int fromIndex = Math.max(sortedMessages.size() - limit, 0);

        return new ArrayList<>(sortedMessages.subList(fromIndex, sortedMessages.size()));
    }

    @Override
    public List<ChatMessageDTO> getMessagesAfterIndex(String conversationId, int messageIndex, int limit) {
        if (limit <= 0) {
            return new ArrayList<>();
        }

        List<ChatMessageDTO> messages = memory.getOrDefault(conversationId, new ArrayList<>());

        return messages.stream()
                .filter(message -> message.getMessageIndex() != null)
                .filter(message -> message.getMessageIndex() > messageIndex)
                .sorted(Comparator.comparing(ChatMessageDTO::getMessageIndex))
                .limit(limit)
                .toList();
    }

    @Override
    public void addMessage(String conversationId, ChatMessageDTO message) {
        memory.computeIfAbsent(conversationId, key -> new ArrayList<>()).add(message);
    }

    @Override
    public void addMessages(String conversationId, List<ChatMessageDTO> messages) {
        memory.computeIfAbsent(conversationId, key -> new ArrayList<>()).addAll(messages);
    }

    @Override
    public void clear(String conversationId) {
        memory.remove(conversationId);
        summaryMemory.remove(conversationId);
        titleMemory.remove(conversationId);
        lastSummaryMessageIndexMemory.remove(conversationId);
        messageIndexMemory.remove(conversationId);
    }

    @Override
    public String getSummary(String conversationId) {
        return summaryMemory.getOrDefault(conversationId, "");
    }

    @Override
    public void updateSummary(String conversationId, String summary) {
        summaryMemory.put(conversationId, summary);
    }

    @Override
    public String getTitle(String conversationId) {
        return titleMemory.getOrDefault(conversationId, "新会话");
    }

    @Override
    public void updateTitle(String conversationId, String title) {
        if (title == null || title.isBlank()) {
            titleMemory.put(conversationId, "新会话");
            return;
        }

        titleMemory.put(conversationId, title);
    }

    @Override
    public boolean hasTitle(String conversationId) {
        String title = titleMemory.get(conversationId);
        return title != null && !title.isBlank();
    }

    @Override
    public int countMessages(String conversationId) {
        return memory.getOrDefault(conversationId, new ArrayList<>()).size();
    }

    @Override
    public int nextMessageIndex(String conversationId) {
        return messageIndexMemory
                .computeIfAbsent(conversationId, key -> new AtomicInteger(0))
                .incrementAndGet();
    }

    @Override
    public int getCurrentMessageIndex(String conversationId) {
        return messageIndexMemory
                .getOrDefault(conversationId, new AtomicInteger(0))
                .get();
    }

    @Override
    public int getLastSummaryMessageIndex(String conversationId) {
        return lastSummaryMessageIndexMemory.getOrDefault(conversationId, 0);
    }

    @Override
    public void updateLastSummaryMessageIndex(String conversationId, int messageIndex) {
        lastSummaryMessageIndexMemory.put(conversationId, messageIndex);
    }

    @Override
    public void trimMessages(String conversationId, int limit) {
        if (limit <= 0) {
            return;
        }

        List<ChatMessageDTO> messages = memory.get(conversationId);

        if (messages == null || messages.size() <= limit) {
            return;
        }

        List<ChatMessageDTO> sortedMessages = messages.stream()
                .filter(message -> message.getMessageIndex() != null)
                .sorted(Comparator.comparing(ChatMessageDTO::getMessageIndex))
                .toList();

        int fromIndex = Math.max(sortedMessages.size() - limit, 0);

        List<ChatMessageDTO> recentMessages = new ArrayList<>(
                sortedMessages.subList(fromIndex, sortedMessages.size())
        );

        memory.put(conversationId, recentMessages);
    }
}