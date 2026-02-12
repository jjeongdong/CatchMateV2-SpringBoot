package com.back.catchmate.infrastructure.redis;

import com.back.catchmate.application.chat.event.ChatMessageEvent;
import com.back.catchmate.domain.chat.model.ChatMessage;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Service;

import java.util.Map;

@Slf4j
@Service
@RequiredArgsConstructor
public class RedisSubscriber {

    private final ObjectMapper objectMapper;
    private final SimpMessagingTemplate messagingTemplate; // WebSocket 전송 도구

    /**
     * RedisConfig에서 매핑한 메서드 이름과 일치해야 함 ("onMessage")
     */
    public void onMessage(String messageJson) {
        try {
            // 1. Redis에서 받은 JSON 문자열 -> Java 객체로 변환
            ChatMessageEvent chatMessage = objectMapper.readValue(messageJson, ChatMessageEvent.class);

            // 2. WebSocket을 통해 해당 채팅방(/sub/chat/room/{id}) 구독자들에게 전송
            // 이 코드는 모든 서버에서 실행되므로, 
            // 현재 서버에 접속해 있는 해당 방의 유저들에게만 메시지가 전달됨.
            messagingTemplate.convertAndSend("/sub/chat/room/" + chatMessage.getRoomId(), chatMessage);

            log.info("Redis Sub -> WebSocket Sent: roomId={}", chatMessage.getRoomId());

        } catch (Exception e) {
            log.error("Redis Message Processing Error", e);
        }
    }

    /**
     * 사용자별 알림 전송용 Redis 구독 메서드
     * userId 기반으로 특정 사용자에게 알림 전송
     */
    public void onNotification(String messageJson) {
        try {
            // 1. JSON -> Map or DTO 변환
            // (편의상 Map으로 받거나, NotificationEvent DTO를 만드셔도 됩니다)
            Map<String, Object> payload = objectMapper.readValue(messageJson, Map.class);

            String userId = String.valueOf(payload.get("userId"));
            Map<String, String> data = (Map<String, String>) payload.get("data");

            // 2. WebSocket 전송 (User Destination)
            // 이 서버에 userId 세션이 있으면 전송되고, 없으면 무시됨 (정상)
            messagingTemplate.convertAndSendToUser(
                    userId,
                    "/queue/notifications",
                    data
            );

            log.info("Redis Sub -> WebSocket Notification Sent to User {}", userId);

        } catch (Exception e) {
            log.error("Redis Notification Processing Error", e);
        }
    }
}
