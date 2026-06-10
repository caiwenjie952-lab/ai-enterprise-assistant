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

    @Override
    public List<ChatMessageDTO> getMessages(String conversationId) {
        return new ArrayList<>(memory.getOrDefault(conversationId, new ArrayList<>()));
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
    }
}