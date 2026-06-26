package com.wenjie.aiassistant.memory.impl;

import com.wenjie.aiassistant.dto.ChatMessageDTO;
import com.wenjie.aiassistant.memory.ConversationMemoryService;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ConcurrentHashMap;

@Service
public class InMemoryConversationMemoryServiceImpl implements ConversationMemoryService {

    private final ConcurrentHashMap<String, List<ChatMessageDTO>> memory = new ConcurrentHashMap<>();

    private final ConcurrentHashMap<String, String> summaryMemory = new ConcurrentHashMap<>();

    private final ConcurrentHashMap<String, Integer> lastSummaryMessageCountMemory = new ConcurrentHashMap<>();

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

        int fromIndex = Math.max(messages.size() - limit, 0);
        return new ArrayList<>(messages.subList(fromIndex, messages.size()));
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
        lastSummaryMessageCountMemory.remove(conversationId);
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
    public int countMessages(String conversationId) {
        return memory.getOrDefault(conversationId, new ArrayList<>()).size();
    }

    @Override
    public int getLastSummaryMessageCount(String conversationId) {
        return lastSummaryMessageCountMemory.getOrDefault(conversationId, 0);
    }

    @Override
    public void updateLastSummaryMessageCount(String conversationId, int messageCount) {
        lastSummaryMessageCountMemory.put(conversationId, messageCount);
    }
}