package com.back.catchmate.notification.application.event;

import com.back.catchmate.notice.domain.event.NoticeCreatedEvent;
import com.back.catchmate.notification.application.NoticeNotificationService;
import lombok.RequiredArgsConstructor;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

@Component
@RequiredArgsConstructor
public class NotificationNoticeCreatedListener {
    private final NoticeNotificationService noticeNotificationService;

    // 공지 등록과 같은 트랜잭션에서 알림·아웃박스를 쌓아야 "공지는 올라갔는데 알림이 없는" 상태가 생기지 않는다.
    @TransactionalEventListener(phase = TransactionPhase.BEFORE_COMMIT)
    public void handle(NoticeCreatedEvent event) {
        noticeNotificationService.saveOnNoticeCreated(event.noticeId(), event.noticeTitle());
    }

    // 롤백된 공지를 알리지 않도록 커밋 후에, 요청 스레드를 잡지 않도록 비동기로 보낸다.
    @Async("notificationDispatchExecutor")
    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void dispatch(NoticeCreatedEvent event) {
        noticeNotificationService.dispatchOnNoticeCreated(event.noticeId(), event.noticeTitle());
    }
}
