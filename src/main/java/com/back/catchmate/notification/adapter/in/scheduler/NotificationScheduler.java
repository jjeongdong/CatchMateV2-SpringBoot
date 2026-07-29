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

    // 발송 결과를 기록하지 못하고 죽은 행(PROCESSING 정체)을 재시도 대상으로 되돌린다.
    // 정체는 인스턴스 재기동·OOM 같은 예외 상황에서만 생기므로 본 발송보다 훨씬 낮은 주기로 돈다.
    @Scheduled(fixedDelayString = "${notification.outbox.recovery-delay-ms:300000}")
    public void recoverStuckProcessing() {
        outboxDispatchUseCase.recoverStuckProcessing();
    }
}
