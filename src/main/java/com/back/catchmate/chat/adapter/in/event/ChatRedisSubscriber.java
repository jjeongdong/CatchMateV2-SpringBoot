package com.back.catchmate.chat.adapter.in.event;

import com.back.catchmate.chat.application.event.ChatMessageEvent;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@RequiredArgsConstructor
public class ChatRedisSubscriber {
    private final ObjectMapper objectMapper;
    private final SimpMessagingTemplate messagingTemplate;

    public void onMessage(String messageJson) {
        try {
            ChatMessageEvent chatMessage = objectMapper.readValue(messageJson, ChatMessageEvent.class);
            messagingTemplate.convertAndSend("/sub/chat/room/" + chatMessage.roomId(), chatMessage);
            log.info("Redis Sub -> WebSocket Sent: roomId={}", chatMessage.roomId());
        } catch (Exception e) {
            log.error("Redis Chat Message Processing Error", e);
        }
    }
}
