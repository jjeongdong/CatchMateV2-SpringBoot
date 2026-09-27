package com.back.catchmate.notification.application.event;

import com.back.catchmate.inquiry.domain.event.InquiryAnswerRegisteredEvent;
import com.back.catchmate.notification.application.InquiryNotificationService;
import lombok.RequiredArgsConstructor;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

@Component
@RequiredArgsConstructor
public class NotificationInquiryAnswerRegisteredListener {
    private final InquiryNotificationService inquiryNotificationService;

    // 답변 등록과 같은 트랜잭션에서 알림·아웃박스를 쌓아야 "답변은 됐는데 알림이 없는" 상태가 생기지 않는다.
    @TransactionalEventListener(phase = TransactionPhase.BEFORE_COMMIT)
    public void handle(InquiryAnswerRegisteredEvent event) {
        inquiryNotificationService.saveOnInquiryAnswered(event.inquiryId(), event.inquiryAuthorId());
    }

    // 롤백된 답변을 알리지 않도록 커밋 후에, FCM 호출 동안 요청 스레드·커넥션을 잡지 않도록 비동기로 보낸다.
    @Async("notificationDispatchExecutor")
    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void dispatch(InquiryAnswerRegisteredEvent event) {
        inquiryNotificationService.dispatchOnInquiryAnswered(event.inquiryId(), event.inquiryAuthorId());
    }
}
