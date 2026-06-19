package com.back.catchmate.chat.adapter.in.scheduler;

import com.back.catchmate.chat.application.port.in.ChatInternalCommandUseCase;
import lombok.RequiredArgsConstructor;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class ReadSequenceFlushScheduler {
    private final ChatInternalCommandUseCase chatInternalCommandUseCase;

    @Scheduled(fixedDelayString = "${chat.read-sequence.flush-delay-ms:5000}")
    public void flushReadSequences() {
        chatInternalCommandUseCase.flushReadSequences();
    }
}
