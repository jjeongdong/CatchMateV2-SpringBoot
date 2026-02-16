package com.back.catchmate.application.chat.event;

import com.back.catchmate.application.notification.event.NotificationEvent;
import com.back.catchmate.application.notification.service.NotificationRetryService;
import com.back.catchmate.domain.notification.port.NotificationSenderPort;
import com.back.catchmate.domain.user.model.User;
import com.back.catchmate.domain.user.port.UserOnlineStatusPort;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

import java.util.Map;

/**
 * 채팅 메시지 알림 이벤트 리스너
 * 사용자의 온라인 상태를 확인하여 Redis Pub/Sub 또는 FCM 발송을 결정하며,
 * FCM 발송 실패 시 DLQ(Dead Letter Queue)에 저장하여 재시도를 보장함
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class ChatNotificationEventListener {
    private final NotificationRetryService notificationRetryService;
    private final UserOnlineStatusPort userOnlineStatusPort;
    private final NotificationSenderPort notificationSenderPort;
    private final ApplicationEventPublisher applicationEventPublisher;

    @Async
    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void handleChatNotification(ChatNotificationEvent event) {
        Map<String, String> payload = createNotificationData(event);

        for (User recipient : event.recipients()) {
            if (recipient.getChatAlarm() != 'Y') {
                continue;
            }

            boolean isOnline = userOnlineStatusPort.isUserOnline(recipient.getId()); 

            if (isOnline) {
                sendWebSocketNotification(recipient.getId(), payload);
            } else {
                sendFcmNotificationWithFallback(recipient, event, payload);
            }
        }
    }

    private static Map<String, String> createNotificationData(ChatNotificationEvent event) {
        return Map.of(
                "type", "CHAT",
                "roomId", event.chatMessage().getChatRoom().getId().toString(),
                "senderId", event.chatMessage().getSender().getId().toString(),
                "senderNickname", event.chatMessage().getSender().getNickName(),
                "content", event.chatMessage().getContent()
        );
    }

    private void sendWebSocketNotification(Long userId, Map<String, String> payload) {
        try {
            applicationEventPublisher.publishEvent(NotificationEvent.of(userId, payload));
        } catch (Exception e) {
            log.warn("WebSocket notification failed. userId: {}", userId, e);
        }
    }

    private void sendFcmNotificationWithFallback(User recipient, ChatNotificationEvent event, Map<String, String> payload) {
        if (recipient.getFcmToken() == null) return;

        try {
            notificationSenderPort.sendNotification(
                    recipient.getFcmToken(),
                    event.title(),
                    event.body(),
                    payload
            );
        } catch (Exception e) {
            log.warn("채팅 푸시 전송 실패 -> DLQ 저장 시도. recipientId: {}", recipient.getId());
            saveToDeadLetterQueue(recipient, event, payload);
        }
    }

    private void saveToDeadLetterQueue(User recipient, ChatNotificationEvent event, Map<String, String> payload) {
        try {
            // 실패한 알림 정보를 DB에 저장하여 스케줄러가 재시도할 수 있게 함
            notificationRetryService.saveFailedNotification(
                    recipient.getId(),
                    recipient.getFcmToken(),
                    event.title(),
                    event.body(),
                    payload
            );
        } catch (Exception dlqError) {
            log.error("DLQ 저장조차 실패했습니다. recipientId: {}", recipient.getId(), dlqError);
        }
    }
}
