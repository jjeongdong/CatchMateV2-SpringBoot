package com.back.catchmate.notification.application.event;

import com.back.catchmate.enroll.domain.event.EnrollAcceptedEvent;
import com.back.catchmate.notification.application.EnrollNotificationService;
import lombok.RequiredArgsConstructor;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

@Component
@RequiredArgsConstructor
public class NotificationEnrollAcceptedListener {
    private final EnrollNotificationService enrollNotificationService;

    // 수락과 같은 트랜잭션에서 알림·아웃박스를 쌓아야 "수락은 됐는데 알림이 없는" 상태가 생기지 않는다 (아웃박스 원자성).
    @TransactionalEventListener(phase = TransactionPhase.BEFORE_COMMIT)
    public void handle(EnrollAcceptedEvent event) {
        enrollNotificationService.saveOnEnrollAccepted(
                event.enrollId(), event.boardId(), event.applicantId(), event.boardOwnerId());
    }

    // 롤백된 수락을 알리지 않도록 커밋 후에, FCM 호출 동안 요청 스레드·커넥션을 잡지 않도록 비동기로 보낸다.
    @Async("notificationDispatchExecutor")
    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void dispatch(EnrollAcceptedEvent event) {
        enrollNotificationService.dispatchOnEnrollAccepted(
                event.enrollId(), event.boardId(), event.applicantId(), event.boardOwnerId());
    }
}
