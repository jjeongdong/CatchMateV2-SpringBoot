package com.back.catchmate.chat.scheduler;

import com.back.catchmate.chat.service.ChatCommandService;
import lombok.RequiredArgsConstructor;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class ReadSequenceFlushScheduler {
    private final ChatCommandService chatCommandService;

    @Scheduled(fixedDelayString = "${chat.read-sequence.flush-delay-ms:5000}")
    public void flushReadSequences() {
        chatCommandService.flushReadSequences();
    }
}
