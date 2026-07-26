package com.back.catchmate.notification.adapter.in.event;

import com.back.catchmate.chat.application.event.ChatMessageNotificationEvent;
import com.back.catchmate.notification.application.port.in.ChatNotificationDispatchUseCase;
import com.back.catchmate.notification.application.port.in.ChatNotificationUseCase;
import lombok.RequiredArgsConstructor;
import org.springframework.context.event.EventListener;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

@Component
@RequiredArgsConstructor
public class ChatNotificationEventListener {
    private final ChatNotificationUseCase chatNotificationUseCase;
    private final ChatNotificationDispatchUseCase chatNotificationDispatchUseCase;

    @EventListener
    public void onSave(ChatMessageNotificationEvent event) {
        chatNotificationUseCase.saveOnChatMessageSent(
                event.chatRoomId(), event.messageId(), event.senderId(), event.content()
        );
    }

    @Async("notificationDispatchExecutor")
    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void onDispatch(ChatMessageNotificationEvent event) {
        chatNotificationDispatchUseCase.dispatchOnChatMessageSent(
                event.chatRoomId(), event.messageId(), event.senderId(), event.content()
        );
    }
}
