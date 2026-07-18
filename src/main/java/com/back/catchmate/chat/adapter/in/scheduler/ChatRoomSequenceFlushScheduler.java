package com.back.catchmate.chat.adapter.in.scheduler;

import com.back.catchmate.chat.application.port.in.ChatInternalCommandUseCase;
import lombok.RequiredArgsConstructor;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class ChatRoomSequenceFlushScheduler {
    private final ChatInternalCommandUseCase chatInternalCommandUseCase;

    @Scheduled(fixedDelayString = "${chat.room-sequence.flush-delay-ms:1000}")
    public void flushRoomSequences() {
        chatInternalCommandUseCase.flushRoomSequences();
    }
}
