package com.back.catchmate.application.admin.event;

import com.back.catchmate.domain.inquiry.model.Inquiry;
import com.back.catchmate.domain.notification.port.NotificationSender;
import com.back.catchmate.domain.user.model.User;
import lombok.RequiredArgsConstructor;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;
import org.springframework.transaction.event.TransactionalEventListener;
import org.springframework.transaction.event.TransactionPhase;

import java.util.Map;

@Component
@RequiredArgsConstructor
public class AdminInquiryAnswerNotificationEventListener {

    private final NotificationSender notificationSender;

    @Async
    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void handle(AdminInquiryAnswerNotificationEvent event) {
        User recipient = event.recipient();
        Inquiry inquiry = event.inquiry();

        if (recipient.getFcmToken() == null || recipient.getEventAlarm() != 'Y') {
            return;
        }

        Map<String, String> data = Map.of(
                "type", event.type(),
                "inquiryId", inquiry.getId().toString()
        );

        notificationSender.sendNotification(
                recipient.getFcmToken(),
                event.title(),
                event.body(),
                data
        );
    }
}

