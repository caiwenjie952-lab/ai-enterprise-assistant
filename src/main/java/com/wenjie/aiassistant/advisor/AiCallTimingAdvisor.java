package com.wenjie.aiassistant.advisor;

import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.chat.client.ChatClientRequest;
import org.springframework.ai.chat.client.ChatClientResponse;
import org.springframework.ai.chat.client.advisor.api.CallAdvisor;
import org.springframework.ai.chat.client.advisor.api.CallAdvisorChain;
import org.springframework.ai.chat.client.advisor.api.StreamAdvisor;
import org.springframework.ai.chat.client.advisor.api.StreamAdvisorChain;
import org.springframework.stereotype.Component;
import reactor.core.publisher.Flux;

@Slf4j
@Component
public class AiCallTimingAdvisor implements CallAdvisor, StreamAdvisor {

    @Override
    public String getName() {
        return "AiCallTimingAdvisor";
    }

    @Override
    public int getOrder() {
        return 0;
    }

    @Override
    public ChatClientResponse adviseCall(ChatClientRequest request, CallAdvisorChain chain) {
        long start = System.currentTimeMillis();

        try {
            return chain.nextCall(request);
        } finally {
            long cost = System.currentTimeMillis() - start;
            log.info("AI同步调用完成，耗时={}ms", cost);
        }
    }

    @Override
    public Flux<ChatClientResponse> adviseStream(ChatClientRequest request, StreamAdvisorChain chain) {
        long start = System.currentTimeMillis();

        return chain.nextStream(request).doFinally(signalType -> {
            long cost = System.currentTimeMillis() - start;
            log.info("AI流式调用完成，耗时={}ms", cost);
        });
    }
}