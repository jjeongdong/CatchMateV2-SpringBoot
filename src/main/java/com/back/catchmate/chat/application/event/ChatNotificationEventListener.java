package com.back.catchmate.chat.application.event;

import com.back.catchmate.notification.application.port.out.NotificationDispatchPort;
import com.back.catchmate.notification.application.service.NotificationRetryService;
import com.back.catchmate.user.domain.model.User;
import com.back.catchmate.user.application.port.out.UserOnlineStatusPort;
import com.back.catchmate.notification.domain.enums.NotificationChannel;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
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
    private final NotificationDispatchPort notificationDispatchPort;

    /**
     * 메인 트랜잭션 내에서 실행되어 아웃박스에 저장함
     */
    @EventListener
    public void saveNotification(ChatNotificationEvent event) {
        Map<String, String> payload = createNotificationData(event);
        for (User recipient : event.recipients()) {
            if (!recipient.isChatAlarmEnabled()) continue;
            // 아웃박스에 PENDING 상태로 저장 (트랜잭션 내)
            notificationRetryService.saveOutbox(
                    recipient.getId(),
                    recipient.getFcmToken(),
                    NotificationChannel.FCM,
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
    @Async("taskExecutor")
    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void handleChatNotification(ChatNotificationEvent event) {
        Map<String, String> payload = createNotificationData(event);
        Long messageRoomId = event.chatMessage().getChatRoomId();

        for (User recipient : event.recipients()) {
            if (!recipient.isChatAlarmEnabled()) continue;

            // 해당 채팅방을 보고 있는 사용자는 채팅방 브로드캐스트로 이미 메시지를 받음 → 별도 알림 스킵
            Long focusRoomId = userOnlineStatusPort.getUserFocusRoom(recipient.getId());
            if (messageRoomId.equals(focusRoomId)) continue;

            // WebSocket 은 항상 시도 — 구독자가 있으면 전달되고, 없으면 silently drop 됨
            notificationDispatchPort.dispatch(recipient.getId(), payload);

            // 오프라인이면 FCM 으로 보강 발송
            if (!userOnlineStatusPort.isUserOnline(recipient.getId())) {
                notificationRetryService.sendPendingOutboxImmediately(recipient.getId());
            }
        }
    }

    private static Map<String, String> createNotificationData(ChatNotificationEvent event) {
        return Map.of(
                "type", "CHAT",
                "roomId", event.chatMessage().getChatRoomId().toString(),
                "senderId", event.sender().getId().toString(),
                "senderNickname", event.sender().getNickName(),
                "content", event.chatMessage().getContent()
        );
    }
}
