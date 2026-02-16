package com.back.catchmate.infrastructure.redis;

import com.back.catchmate.application.chat.event.ChatMessageEvent;
import com.back.catchmate.application.notification.event.NotificationEvent;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Service;

@Slf4j
@Service
@RequiredArgsConstructor
public class RedisSubscriber {
    private final ObjectMapper objectMapper;
    private final SimpMessagingTemplate messagingTemplate;

    /**
     * 채팅 메시지 수신 처리 메서드
     * RedisConfig에서 매핑한 메서드 이름과 일치해야 함 ("onMessage")
     */
    public void onMessage(String messageJson) {
        try {
            // 1. Redis에서 받은 JSON 문자열 -> Java 객체로 변환
            ChatMessageEvent chatMessage = objectMapper.readValue(messageJson, ChatMessageEvent.class);

            // 2. WebSocket을 통해 해당 채팅방(/sub/chat/room/{id}) 구독자들에게 전송
            // 이 코드는 모든 서버에서 실행되므로, 
            // 현재 서버에 접속해 있는 해당 방의 유저들에게만 메시지가 전달됨.
            messagingTemplate.convertAndSend("/sub/chat/room/" + chatMessage.roomId(), chatMessage);

            log.info("Redis Sub -> WebSocket Sent: roomId={}", chatMessage.roomId());
        } catch (Exception e) {
            log.error("Redis Message Processing Error", e);
        }
    }

    /**
     * 알림 메시지 수신 처리 메서드
     * RedisConfig에서 매핑한 메서드 이름과 일치해야 함 ("onNotification")
     */
    public void onNotification(String messageJson) {
        log.info("Redis에서 수신한 원본 메시지: {}", messageJson);
        try {
            NotificationEvent event = objectMapper.readValue(messageJson, NotificationEvent.class);

            // WebSocket 전송
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
