package com.back.catchmate.application.chat.event;

import com.back.catchmate.application.chat.port.MessagePublisherPort;
import com.back.catchmate.application.notification.event.NotificationEvent;
import com.back.catchmate.domain.notification.port.NotificationSenderPort;
import com.back.catchmate.domain.user.model.User;
import com.back.catchmate.domain.user.port.UserOnlineStatusPort;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

import java.util.Map;

/**
 * 채팅 메시지 FCM 알림 이벤트 리스너
 * 오프라인 사용자에게만 Push 알림 전송
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class ChatNotificationEventListener {
    private final NotificationSenderPort notificationSenderPort;
    private final UserOnlineStatusPort userOnlineStatusPort;
    private final MessagePublisherPort messagePublisherPort;

    @Async
    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void handleChatNotification(ChatNotificationEvent event) {
        Map<String, String> data = Map.of(
                "type", "CHAT",
                "roomId", event.chatMessage().getChatRoom().getId().toString(),
                "senderId", event.chatMessage().getSender().getId().toString(),
                "senderNickname", event.chatMessage().getSender().getNickName(),
                "content", event.chatMessage().getContent()
        );

        // 2. 수신자 목록 순회 (이미 발신자는 제외된 리스트라고 가정)
        for (User recipient : event.recipients()) {
            try {
                if (recipient.getChatAlarm() != 'Y') continue;
                boolean isOnline = userOnlineStatusPort.isUserOnline(recipient.getId());

                if (isOnline) {
                    messagePublisherPort.publishNotification(NotificationEvent.of(recipient.getId(), data));
                }

                if (!isOnline && recipient.getFcmToken() != null) {
                    notificationSenderPort.sendNotification(
                            recipient.getFcmToken(),
                            event.title(),
                            event.body(),
                            data
                    );
                }
            } catch (Exception e) {
                log.warn("Failed to send notification to user {}", recipient.getId(), e);
            }
        }
    }
}
