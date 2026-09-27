package com.back.catchmate.notification.application.event;

import com.back.catchmate.chat.domain.event.ChatMessageSentEvent;
import com.back.catchmate.notification.application.ChatNotificationService;
import lombok.RequiredArgsConstructor;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

@Component
@RequiredArgsConstructor
public class NotificationChatMessageSentListener {
    private final ChatNotificationService chatNotificationService;

    // 메시지 저장과 같은 트랜잭션에서 아웃박스를 쌓아야 "메시지는 저장됐는데 푸시가 없는" 상태가 생기지 않는다.
    @TransactionalEventListener(phase = TransactionPhase.BEFORE_COMMIT)
    public void handle(ChatMessageSentEvent event) {
        chatNotificationService.saveOnChatMessageSent(
                event.chatRoomId(), event.messageId(), event.senderId(), event.content());
    }

    // 롤백된 메시지를 알리지 않도록 커밋 후에, FCM 호출 동안 요청 스레드·커넥션을 잡지 않도록 비동기로 보낸다.
    @Async("notificationDispatchExecutor")
    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void dispatch(ChatMessageSentEvent event) {
        chatNotificationService.dispatchOnChatMessageSent(
                event.chatRoomId(), event.messageId(), event.senderId(), event.content());
    }
}
