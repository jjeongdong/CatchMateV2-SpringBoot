package com.back.catchmate.application.chat.event;

import com.back.catchmate.domain.notification.port.NotificationSender;
import com.back.catchmate.domain.user.model.User;
import lombok.RequiredArgsConstructor;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

import java.util.Map;

/**
 * 채팅 메시지 FCM 알림 이벤트 리스너
 * 오프라인 사용자에게만 Push 알림 전송
 */
@Component
@RequiredArgsConstructor
public class ChatNotificationEventListener {
    private final NotificationSender notificationSender;

    @Async
    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void handleChatNotification(ChatNotificationEvent event) {
        for (User recipient : event.recipients()) {
            // FCM 토큰이 없거나 채팅 알림이 꺼져있으면 스킵
            if (recipient.getFcmToken() == null || recipient.getChatAlarm() != 'Y') {
                continue;
            }

            Map<String, String> data = Map.of(
                    "type", "CHAT_MESSAGE",
                    "chatRoomId", event.chatMessage().getChatRoom().getId().toString(),
                    "messageId", event.chatMessage().getId().toString()
            );

            // 오프라인 사용자에게만 FCM 알림 전송
            notificationSender.sendNotificationIfOffline(
                    recipient.getId(),
                    recipient.getFcmToken(),
                    event.title(),
                    event.body(),
                    data
            );
        }
    }
}
