package com.back.catchmate.global.scheduler;

import com.back.catchmate.chat.application.port.in.ChatUseCase;
import lombok.RequiredArgsConstructor;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class ReadSequenceFlushScheduler {
    private final ChatUseCase chatOrchestrator;

    @Scheduled(fixedDelayString = "${chat.read-sequence.flush-delay-ms:5000}")
    public void flushReadSequences() {
        chatOrchestrator.flushReadSequences();
    }
}
