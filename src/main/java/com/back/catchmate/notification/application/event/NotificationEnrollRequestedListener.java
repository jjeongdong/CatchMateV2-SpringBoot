package com.back.catchmate.notification.application.event;

import com.back.catchmate.enroll.domain.event.EnrollRequestedEvent;
import com.back.catchmate.notification.application.EnrollNotificationService;
import lombok.RequiredArgsConstructor;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

@Component
@RequiredArgsConstructor
public class NotificationEnrollRequestedListener {
    private final EnrollNotificationService enrollNotificationService;

    // 신청과 같은 트랜잭션에서 알림·아웃박스를 쌓아야 "신청은 됐는데 알림이 없는" 상태가 생기지 않는다 (아웃박스 원자성).
    @TransactionalEventListener(phase = TransactionPhase.BEFORE_COMMIT)
    public void handle(EnrollRequestedEvent event) {
        enrollNotificationService.saveOnEnrollRequested(
                event.enrollId(), event.boardId(), event.applicantId(), event.boardOwnerId());
    }

    // 롤백된 신청을 알리지 않도록 커밋 후에, FCM 호출 동안 요청 스레드·커넥션을 잡지 않도록 비동기로 보낸다.
    @Async("notificationDispatchExecutor")
    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void dispatch(EnrollRequestedEvent event) {
        enrollNotificationService.dispatchOnEnrollRequested(
                event.enrollId(), event.boardId(), event.applicantId(), event.boardOwnerId());
    }
}
