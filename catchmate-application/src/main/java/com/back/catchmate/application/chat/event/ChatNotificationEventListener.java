package com.back.catchmate.application.chat.event;

import com.back.catchmate.application.chat.port.MessagePublisher;
import com.back.catchmate.domain.notification.port.NotificationSender;
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
    private final NotificationSender notificationSender; // FCM 알림 전송
    private final UserOnlineStatusPort userOnlineStatusPort; // 온라인 상태 확인
    private final MessagePublisher messagePublisher; // WebSocket 알림 전송

    @Async
    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void handleChatNotification(ChatNotificationEvent event) {
        Map<String, String> data = Map.of(
                "type", "CHAT",
                "roomId", event.chatMessage().getChatRoom().getId().toString(), // 채팅방 ID
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
                    messagePublisher.publishNotification(recipient.getId(), data);
                }

                if (!isOnline && recipient.getFcmToken() != null) {
                    notificationSender.sendNotification(
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
