package com.back.catchmate.notification.adapter.in.scheduler;

import com.back.catchmate.notification.application.port.in.OutboxDispatchUseCase;
import lombok.RequiredArgsConstructor;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class NotificationScheduler {
    private final OutboxDispatchUseCase outboxDispatchUseCase;

    @Scheduled(fixedDelayString = "${notification.outbox.scheduler-delay-ms:60000}")
    public void processPendingPush() {
        outboxDispatchUseCase.processPendingNotifications();
    }
}
