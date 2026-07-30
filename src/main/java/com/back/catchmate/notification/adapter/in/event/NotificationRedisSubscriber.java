package com.back.catchmate.notification.adapter.in.event;

import com.back.catchmate.notification.application.event.NotificationEvent;
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
            NotificationEvent event = objectMapper.readValue(messageJson, NotificationEvent.class);
            // 한 메시지에 수신자가 여러 명 담겨 온다. 여기서의 분배는 메모리 작업이며,
            // 자기 인스턴스에 세션이 없는 수신자는 Spring 이 알아서 버린다(세션은 한 인스턴스에만 붙는다).
            for (Long userId : event.userIds()) {
                messagingTemplate.convertAndSendToUser(
                        String.valueOf(userId),
                        "/queue/notifications",
                        event.data()
                );
            }
            log.debug("Redis Sub -> WebSocket Notification 분배 완료. 수신자 {}명", event.userIds().size());
        } catch (Exception e) {
            log.error("Redis Notification Processing Error", e);
        }
    }
}
