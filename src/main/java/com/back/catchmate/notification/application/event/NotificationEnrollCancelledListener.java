package com.back.catchmate.notification.application.event;

import com.back.catchmate.enroll.domain.event.EnrollCancelledEvent;
import com.back.catchmate.notification.application.EnrollNotificationService;
import lombok.RequiredArgsConstructor;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

@Component
@RequiredArgsConstructor
public class NotificationEnrollCancelledListener {
    private final EnrollNotificationService enrollNotificationService;

    // 취소와 같은 트랜잭션에서 인앱 알림을 남겨야 "취소는 됐는데 알림이 없는" 상태가 생기지 않는다.
    @TransactionalEventListener(phase = TransactionPhase.BEFORE_COMMIT)
    public void handle(EnrollCancelledEvent event) {
        enrollNotificationService.saveOnEnrollCancelled(
                event.enrollId(), event.boardId(), event.applicantId(), event.boardOwnerId());
    }

    // 롤백된 취소를 알리지 않도록 커밋 후에, 요청 스레드를 잡지 않도록 비동기로 보낸다.
    @Async("notificationDispatchExecutor")
    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void dispatch(EnrollCancelledEvent event) {
        enrollNotificationService.dispatchOnEnrollCancelled(
                event.enrollId(), event.boardId(), event.applicantId(), event.boardOwnerId());
    }
}
