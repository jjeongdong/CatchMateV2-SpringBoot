package com.back.catchmate.chat.scheduler;

import com.back.catchmate.chat.service.ChatCommandService;
import lombok.RequiredArgsConstructor;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class ChatRoomSequenceFlushScheduler {
    private final ChatCommandService chatCommandService;

    @Scheduled(fixedDelayString = "${chat.room-sequence.flush-delay-ms:1000}")
    public void flushRoomSequences() {
        chatCommandService.flushRoomSequences();
    }
}
