package com.back.catchmate.global.scheduler;

import com.back.catchmate.chat.application.service.ChatOrchestrator;
import lombok.RequiredArgsConstructor;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class ChatMessageFlushScheduler {
    private final ChatOrchestrator chatOrchestrator;

    @Scheduled(fixedDelayString = "${chat.message.flush-delay-ms:1000}")
    public void flushMessages() {
        chatOrchestrator.flushMessages();
    }
}
