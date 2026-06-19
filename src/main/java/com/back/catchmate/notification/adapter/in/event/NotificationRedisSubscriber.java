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
            messagingTemplate.convertAndSendToUser(
                    String.valueOf(event.userId()),
                    "/queue/notifications",
                    event.data()
            );
            log.info("Redis Sub -> WebSocket Notification Sent to User {}", event.userId());
        } catch (Exception e) {
            log.error("Redis Notification Processing Error", e);
        }
    }
}
