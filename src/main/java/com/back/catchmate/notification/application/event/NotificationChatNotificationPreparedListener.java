package com.back.catchmate.notification.application.event;

import com.back.catchmate.notification.application.ChatNotificationService;
import com.back.catchmate.notification.domain.event.ChatNotificationPreparedEvent;
import lombok.RequiredArgsConstructor;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

@Component
@RequiredArgsConstructor
public class NotificationChatNotificationPreparedListener {
    private final ChatNotificationService chatNotificationService;

    // 이 이벤트는 채팅 메시지 트랜잭션의 BEFORE_COMMIT 단계에서 발행된다. 그 시점에도 트랜잭션 동기화가 살아 있어
    // AFTER_COMMIT 으로 등록되고, 롤백되면 발송하지 않는다. FCM 호출 동안 요청 스레드·커넥션을 잡지 않도록 비동기로 보낸다.
    @Async("notificationDispatchExecutor")
    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void dispatch(ChatNotificationPreparedEvent event) {
        chatNotificationService.dispatchOnChatNotificationPrepared(event);
    }
}
