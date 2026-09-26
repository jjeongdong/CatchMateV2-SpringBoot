package com.back.catchmate.chat.application;

import lombok.RequiredArgsConstructor;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class ChatBufferFlushScheduler {
    private final ChatCommandService chatCommandService;

    @Scheduled(fixedDelayString = "${chat.room-sequence.flush-delay-ms:1000}")
    public void flushRoomSequences() {
        chatCommandService.flushRoomSequences();
    }

    @Scheduled(fixedDelayString = "${chat.read-sequence.flush-delay-ms:5000}")
    public void flushReadSequences() {
        chatCommandService.flushReadSequences();
    }
}
