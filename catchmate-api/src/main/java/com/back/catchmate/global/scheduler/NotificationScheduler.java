package com.back.catchmate.global.scheduler;

import com.back.catchmate.orchestration.notification.NotificationOrchestrator;
import lombok.RequiredArgsConstructor;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class NotificationScheduler {
    private final NotificationOrchestrator notificationOrchestrator;

    @Scheduled(fixedDelay = 600000)
    public void retryFailedPush() {
        notificationOrchestrator.retryFailedNotifications();
    }
}
