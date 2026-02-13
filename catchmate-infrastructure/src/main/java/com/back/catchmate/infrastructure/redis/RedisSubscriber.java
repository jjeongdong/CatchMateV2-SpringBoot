package com.back.catchmate.infrastructure.redis;

import com.back.catchmate.application.chat.event.ChatMessageEvent;
import com.back.catchmate.application.chat.event.ChatReadEvent;
import com.back.catchmate.application.notification.event.NotificationEvent;
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
     * 실시간 알림 처리용 Redis 구독 메서드
     *
     * @param messageJson
     */
    public void onNotification(String messageJson) {
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

    /*
     * 채팅 읽음 처리용 Redis 구독 메서드
     * 채팅방 구독자들에게 읽음 이벤트 전파 (roomId 기반)
     * 클라이언트는 이 이벤트를 받아서 해당 방의 읽음 상태를 갱신함
     */
    public void onRead(String messageJson) {
        try {
            ChatReadEvent readEvent = objectMapper.readValue(messageJson, ChatReadEvent.class);

            // 채팅방 구독자들에게 "읽음 이벤트" 전송
            // 클라이언트는 이걸 받으면 -> "roomId가 같으면 뱃지 갱신 / 숫자 1 삭제" 로직 수행
            messagingTemplate.convertAndSend(
                    "/sub/chat/room/" + readEvent.chatRoomId(),
                    readEvent
            );

            log.info("읽음 처리 전파 완료: RoomId={}, UserId={}", readEvent.chatRoomId(), readEvent.userId());
        } catch (Exception e) {
            log.error("Redis Sub 에러 (Read)", e);
        }
    }
}
