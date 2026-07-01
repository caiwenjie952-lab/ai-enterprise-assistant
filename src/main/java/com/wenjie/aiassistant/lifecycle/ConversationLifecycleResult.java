package com.wenjie.aiassistant.lifecycle;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class ConversationLifecycleResult {

    private String summary;

    private Integer currentMessageIndex;

    private Boolean summaryUpdated;

    /**
     * 会话标题
     */
    private String title;

    /**
     * 本轮是否生成了标题
     */
    private Boolean titleGenerated;
}
