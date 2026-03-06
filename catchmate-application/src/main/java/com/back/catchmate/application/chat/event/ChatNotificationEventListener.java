package com.back.catchmate.application.chat.event;

import com.back.catchmate.application.notification.event.NotificationEvent;
import com.back.catchmate.application.notification.service.NotificationRetryService;
import com.back.catchmate.domain.user.model.User;
import com.back.catchmate.domain.user.port.UserOnlineStatusPort;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.context.event.EventListener;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

import java.util.Map;

/**
 * 채팅 메시지 알림 이벤트 리스너
 * Transactional Outbox Pattern 적용:
 * 1. saveNotification: 메인 트랜잭션 내에서 Outbox에 PENDING 상태로 저장
 * 2. handleChatNotification: 커밋 후 즉시 발송 시도 (Best Effort)
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class ChatNotificationEventListener {
    private final NotificationRetryService notificationRetryService;
    private final UserOnlineStatusPort userOnlineStatusPort;
    private final ApplicationEventPublisher applicationEventPublisher;

    /**
     * 메인 트랜잭션 내에서 실행되어 아웃박스에 저장함
     */
    @EventListener
    public void saveNotification(ChatNotificationEvent event) {
        Map<String, String> payload = createNotificationData(event);
        for (User recipient : event.recipients()) {
            if (recipient.getChatAlarm() != 'Y') continue;
            // 아웃박스에 PENDING 상태로 저장 (트랜잭션 내)
            notificationRetryService.saveOutbox(
                    recipient.getId(),
                    recipient.getFcmToken(),
                    event.title(),
                    event.body(),
                    payload
            );
        }
    }

    /**
     * 커밋 후 즉시 전송 시도 (선택 사항, 성능을 위해 비동기 처리)
     * 여기서 실패해도 스케줄러가 처리할 것이므로 안전함
     */
    @Async
    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void handleChatNotification(ChatNotificationEvent event) {
        Map<String, String> payload = createNotificationData(event);
        Long messageRoomId = event.chatMessage().getChatRoom().getId();

        for (User recipient : event.recipients()) {
            if (recipient.getChatAlarm() != 'Y') continue;

            boolean isOnline = userOnlineStatusPort.isUserOnline(recipient.getId());
            Long focusRoomId = userOnlineStatusPort.getUserFocusRoom(recipient.getId());

            if (isOnline) {
                if (!messageRoomId.equals(focusRoomId)) {
                    sendWebSocketNotification(recipient.getId(), payload);
                }
            } else {
                // 커밋 후 즉시 FCM 발송 시도 (Best effort)
                // 실제 아웃박스 상태 업데이트는 하지 않음 (복잡성 방지 위해 스케줄러에 위임하거나, 필요시 업데이트 로직 추가 가능)
                // 여기서는 로그만 남기고 실제 처리는 스케줄러가 담당하도록 비워두거나 간단히 시도만 함
                log.info("채팅 알림 즉시 발송 시도 (Async AFTER_COMMIT): recipientId: {}", recipient.getId());
                // (선택) 여기서 바로 성공 처리 로직을 넣을 수도 있지만, 중복 발송 방지를 위해 스케줄러가 전담하는 것이 가장 깔끔함
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
}
