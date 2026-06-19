package com.back.catchmate.notification.adapter.in.event;

import com.back.catchmate.admin.application.event.NoticeCreatedEvent;
import com.back.catchmate.notification.application.port.in.AdminNoticeNotificationUseCase;
import lombok.RequiredArgsConstructor;
import org.springframework.context.event.EventListener;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

@Component
@RequiredArgsConstructor
public class AdminNoticeNotificationEventListener {
    private final AdminNoticeNotificationUseCase adminNoticeNotificationUseCase;

    @EventListener
    public void onSave(NoticeCreatedEvent event) {
        adminNoticeNotificationUseCase.saveOnNoticeCreated(event.noticeId(), event.noticeTitle());
    }

    @Async("taskExecutor")
    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void onDispatch(NoticeCreatedEvent event) {
        adminNoticeNotificationUseCase.dispatchOnNoticeCreated(event.noticeId(), event.noticeTitle());
    }
}
