package com.back.catchmate.notification.event;

import com.back.catchmate.admin.event.InquiryAnswerRegisteredEvent;
import com.back.catchmate.notification.service.AdminInquiryNotificationDispatchService;
import com.back.catchmate.notification.service.AdminInquiryNotificationService;
import lombok.RequiredArgsConstructor;
import org.springframework.context.event.EventListener;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

@Component
@RequiredArgsConstructor
public class AdminInquiryNotificationEventListener {
    private final AdminInquiryNotificationService adminInquiryNotificationService;
    private final AdminInquiryNotificationDispatchService adminInquiryNotificationDispatchService;

    @EventListener
    public void onSave(InquiryAnswerRegisteredEvent event) {
        adminInquiryNotificationService.saveOnInquiryAnswered(event.inquiryId(), event.inquiryAuthorId());
    }

    @Async("notificationDispatchExecutor")
    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void onDispatch(InquiryAnswerRegisteredEvent event) {
        adminInquiryNotificationDispatchService.dispatchOnInquiryAnswered(event.inquiryId(), event.inquiryAuthorId());
    }
}
