package com.wenjie.aiassistant.context;

import com.wenjie.aiassistant.dto.ChatMessageDTO;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class ChatContext {

    private String conversationId;

    private ChatMessageDTO currentUserMessage;

    private List<ChatMessageDTO> contextMessages;

    private String summary;

}
