package com.back.catchmate.notification.event;

import com.back.catchmate.notice.domain.event.NoticeCreatedEvent;
import com.back.catchmate.notification.service.AdminNoticeNotificationDispatchService;
import com.back.catchmate.notification.service.AdminNoticeNotificationService;
import lombok.RequiredArgsConstructor;
import org.springframework.context.event.EventListener;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

@Component
@RequiredArgsConstructor
public class AdminNoticeNotificationEventListener {
    private final AdminNoticeNotificationService adminNoticeNotificationService;
    private final AdminNoticeNotificationDispatchService adminNoticeNotificationDispatchService;

    @EventListener
    public void onSave(NoticeCreatedEvent event) {
        adminNoticeNotificationService.saveOnNoticeCreated(event.noticeId(), event.noticeTitle());
    }

    @Async("notificationDispatchExecutor")
    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void onDispatch(NoticeCreatedEvent event) {
        adminNoticeNotificationDispatchService.dispatchOnNoticeCreated(event.noticeId(), event.noticeTitle());
    }
}
