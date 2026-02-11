package com.back.catchmate.infrastructure.redis;

import com.back.catchmate.application.chat.event.ChatMessageEvent;
import com.back.catchmate.domain.chat.model.ChatMessage;
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
}
