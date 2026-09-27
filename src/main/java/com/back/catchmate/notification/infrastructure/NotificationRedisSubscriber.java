package com.back.catchmate.notification.infrastructure;

import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@RequiredArgsConstructor
public class NotificationRedisSubscriber {
    private final ObjectMapper objectMapper;
    private final SimpMessagingTemplate messagingTemplate;

    public void onNotification(String messageJson) {
        try {
            RealtimeNotificationPayload payload =
                    objectMapper.readValue(messageJson, RealtimeNotificationPayload.class);
            // 자기 인스턴스에 세션이 없는 수신자는 Spring 이 버린다(세션은 한 인스턴스에만 붙는다).
            for (Long userId : payload.userIds()) {
                messagingTemplate.convertAndSendToUser(String.valueOf(userId), "/queue/notifications", payload.data());
            }
            log.debug("실시간 알림 분배 완료 count={}", payload.userIds().size());
        } catch (Exception e) {
            // 리스너 컨테이너 스레드로 예외를 올리지 않는다 — 한 메시지 때문에 구독이 멈추면 안 된다.
            log.error("실시간 알림 분배 실패", e);
        }
    }
}
