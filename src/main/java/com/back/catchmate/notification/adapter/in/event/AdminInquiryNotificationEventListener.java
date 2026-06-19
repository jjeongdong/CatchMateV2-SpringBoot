package com.back.catchmate.notification.adapter.in.event;

import com.back.catchmate.admin.application.event.InquiryAnswerRegisteredEvent;
import com.back.catchmate.notification.application.port.in.AdminInquiryNotificationUseCase;
import lombok.RequiredArgsConstructor;
import org.springframework.context.event.EventListener;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

@Component
@RequiredArgsConstructor
public class AdminInquiryNotificationEventListener {
    private final AdminInquiryNotificationUseCase adminInquiryNotificationUseCase;

    @EventListener
    public void onSave(InquiryAnswerRegisteredEvent event) {
        adminInquiryNotificationUseCase.saveOnInquiryAnswered(event.inquiryId(), event.inquiryAuthorId());
    }

    @Async("taskExecutor")
    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void onDispatch(InquiryAnswerRegisteredEvent event) {
        adminInquiryNotificationUseCase.dispatchOnInquiryAnswered(event.inquiryId(), event.inquiryAuthorId());
    }
}
