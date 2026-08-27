package com.wenjie.aiassistant.memory.impl;

import com.wenjie.aiassistant.dto.ChatMessageDTO;
import com.wenjie.aiassistant.dto.ConversationListItemResponse;
import com.wenjie.aiassistant.memory.ConversationMemoryService;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;

@Service
public class InMemoryConversationMemoryServiceImpl implements ConversationMemoryService {

    /**
     * 当前会话内存消息
     */
    private final ConcurrentHashMap<String, List<ChatMessageDTO>> memory = new ConcurrentHashMap<>();

    /**
     * 当前会话摘要
     */
    private final ConcurrentHashMap<String, String> summaryMemory = new ConcurrentHashMap<>();

    /**
     * 当前会话标题
     */
    private final ConcurrentHashMap<String, String> titleMemory = new ConcurrentHashMap<>();

    /**
     * 当前会话最新消息编号
     */
    private final ConcurrentHashMap<String, AtomicInteger> messageIndexMemory = new ConcurrentHashMap<>();

    /**
     * 上次生成 summary 时的 messageIndex
     */
    private final ConcurrentHashMap<String, Integer> lastSummaryMessageIndexMemory = new ConcurrentHashMap<>();

    /**
     * 会话创建时间
     */
    private final ConcurrentHashMap<String, Long> conversationCreateTimeMemory = new ConcurrentHashMap<>();

    /**
     * 会话最后更新时间
     */
    private final ConcurrentHashMap<String, Long> conversationUpdateTimeMemory = new ConcurrentHashMap<>();


    @Override
    public List<ChatMessageDTO> getMessages(String conversationId) {
        return new ArrayList<>(memory.getOrDefault(conversationId, new ArrayList<>()));
    }


    @Override
    public List<ChatMessageDTO> getRecentMessages(String conversationId, int limit) {
        if (limit <= 0) {
            return new ArrayList<>();
        }

        List<ChatMessageDTO> messages = memory.getOrDefault(conversationId, new ArrayList<>());

        if (messages.isEmpty()) {
            return new ArrayList<>();
        }

        List<ChatMessageDTO> sortedMessages = messages.stream().filter(message -> message.getMessageIndex() != null).sorted(Comparator.comparing(ChatMessageDTO::getMessageIndex)).toList();

        int fromIndex = Math.max(sortedMessages.size() - limit, 0);

        return new ArrayList<>(sortedMessages.subList(fromIndex, sortedMessages.size()));
    }


    @Override
    public List<ChatMessageDTO> getMessagesAfterIndex(String conversationId, int messageIndex, int limit) {
        if (limit <= 0) {
            return new ArrayList<>();
        }

        List<ChatMessageDTO> messages = memory.getOrDefault(conversationId, new ArrayList<>());

        return messages.stream().filter(message -> message.getMessageIndex() != null).filter(message -> message.getMessageIndex() > messageIndex).sorted(Comparator.comparing(ChatMessageDTO::getMessageIndex)).limit(limit).toList();
    }


    @Override
    public void addMessage(String conversationId, ChatMessageDTO message) {
        memory.computeIfAbsent(conversationId, key -> new ArrayList<>()).add(message);

        touchConversation(conversationId);
    }


    @Override
    public void addMessages(String conversationId, List<ChatMessageDTO> messages) {
        if (messages == null || messages.isEmpty()) {
            return;
        }

        memory.computeIfAbsent(conversationId, key -> new ArrayList<>()).addAll(messages);

        touchConversation(conversationId);
    }


    @Override
    public int countMessages(String conversationId) {
        return memory.getOrDefault(conversationId, new ArrayList<>()).size();
    }


    @Override
    public int nextMessageIndex(String conversationId) {
        return messageIndexMemory.computeIfAbsent(conversationId, key -> new AtomicInteger(0)).incrementAndGet();
    }


    @Override
    public int getCurrentMessageIndex(String conversationId) {
        AtomicInteger index = messageIndexMemory.get(conversationId);

        return index == null ? 0 : index.get();
    }


    @Override
    public String getSummary(String conversationId) {
        return summaryMemory.getOrDefault(conversationId, "");
    }


    @Override
    public void updateSummary(String conversationId, String summary) {
        if (summary == null) {
            summary = "";
        }

        summaryMemory.put(conversationId, summary);

        touchConversation(conversationId);
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
    public String getTitle(String conversationId) {
        return titleMemory.getOrDefault(conversationId, "新会话");
    }


    @Override
    public void updateTitle(String conversationId, String title) {
        if (title == null || title.isBlank()) {
            title = "新会话";
        }

        titleMemory.put(conversationId, title);

        touchConversation(conversationId);
    }


    @Override
    public boolean hasTitle(String conversationId) {
        String title = titleMemory.get(conversationId);

        return title != null && !title.isBlank() && !"新会话".equals(title);
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

        List<ChatMessageDTO> sortedMessages = messages.stream().filter(message -> message.getMessageIndex() != null).sorted(Comparator.comparing(ChatMessageDTO::getMessageIndex)).toList();

        int fromIndex = Math.max(sortedMessages.size() - limit, 0);

        List<ChatMessageDTO> recentMessages = new ArrayList<>(sortedMessages.subList(fromIndex, sortedMessages.size()));

        memory.put(conversationId, recentMessages);
    }


    @Override
    public void clear(String conversationId) {
        memory.remove(conversationId);
        summaryMemory.remove(conversationId);
        titleMemory.remove(conversationId);
        messageIndexMemory.remove(conversationId);
        lastSummaryMessageIndexMemory.remove(conversationId);
        conversationCreateTimeMemory.remove(conversationId);
        conversationUpdateTimeMemory.remove(conversationId);
    }


    @Override
    public boolean exists(String conversationId) {
        if (conversationId == null || conversationId.isBlank()) {
            return false;
        }

        return memory.containsKey(conversationId) || messageIndexMemory.containsKey(conversationId) || summaryMemory.containsKey(conversationId) || titleMemory.containsKey(conversationId);
    }


    @Override
    public void restoreConversation(String conversationId, String title, String summary, int currentMessageIndex, int lastSummaryMessageIndex, List<ChatMessageDTO> messages) {
        List<ChatMessageDTO> safeMessages = messages == null ? new ArrayList<>() : new ArrayList<>(messages);

        // 恢复最近消息
        memory.put(conversationId, safeMessages);

        // 恢复标题
        titleMemory.put(conversationId, title == null || title.isBlank() ? "新会话" : title);

        // 恢复摘要
        summaryMemory.put(conversationId, summary == null ? "" : summary);

        // 最关键：
        // 恢复当前 messageIndex
        messageIndexMemory.put(conversationId, new AtomicInteger(currentMessageIndex));

        // 恢复摘要进度
        lastSummaryMessageIndexMemory.put(conversationId, lastSummaryMessageIndex);

        touchConversation(conversationId);
    }


    @Override
    public List<ConversationListItemResponse> listConversations() {
        return memory.keySet().stream().map(conversationId -> {

            List<ChatMessageDTO> messages = memory.getOrDefault(conversationId, new ArrayList<>());

            ChatMessageDTO lastMessage = messages.stream().filter(message -> message.getMessageIndex() != null).max(Comparator.comparing(ChatMessageDTO::getMessageIndex)).orElse(null);

            String lastMessageContent = lastMessage == null ? "" : lastMessage.getContent();

            if (lastMessageContent != null && lastMessageContent.length() > 50) {
                lastMessageContent = lastMessageContent.substring(0, 50);
            }

            String summary = getSummary(conversationId);

            return new ConversationListItemResponse(conversationId, getTitle(conversationId), messages.size(), getCurrentMessageIndex(conversationId), getLastSummaryMessageIndex(conversationId), summary != null && !summary.isBlank(), lastMessageContent, conversationCreateTimeMemory.getOrDefault(conversationId, 0L), conversationUpdateTimeMemory.getOrDefault(conversationId, 0L));
        }).sorted(Comparator.comparing(ConversationListItemResponse::getUpdateTime).reversed()).toList();
    }


    @Override
    public void touchConversation(String conversationId) {
        if (conversationId == null || conversationId.isBlank()) {
            return;
        }

        long now = System.currentTimeMillis();

        conversationCreateTimeMemory.putIfAbsent(conversationId, now);

        conversationUpdateTimeMemory.put(conversationId, now);
    }
}