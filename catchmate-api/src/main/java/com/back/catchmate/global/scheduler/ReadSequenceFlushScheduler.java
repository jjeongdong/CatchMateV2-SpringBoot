package com.back.catchmate.global.scheduler;

import com.back.catchmate.orchestration.chat.ChatOrchestrator;
import lombok.RequiredArgsConstructor;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class ReadSequenceFlushScheduler {
    private final ChatOrchestrator chatOrchestrator;

    @Scheduled(fixedDelayString = "${chat.read-sequence.flush-delay-ms:5000}")
    public void flushReadSequences() {
        chatOrchestrator.flushReadSequences();
    }
}
