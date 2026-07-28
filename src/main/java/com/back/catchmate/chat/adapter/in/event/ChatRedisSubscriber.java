package com.back.catchmate.chat.adapter.in.event;

import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.core.JsonToken;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.messaging.Message;
import org.springframework.messaging.MessageHeaders;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.messaging.support.MessageBuilder;
import org.springframework.stereotype.Component;
import org.springframework.util.MimeTypeUtils;

import java.io.IOException;

@Slf4j
@Component
@RequiredArgsConstructor
public class ChatRedisSubscriber {
    private final ObjectMapper objectMapper;
    private final SimpMessagingTemplate messagingTemplate;

    // Redis 에서 온 JSON 을 객체로 복원했다가 다시 직렬화하지 않고,
    // 목적지 계산에 필요한 roomId 만 얕게 읽고 원본 바이트를 그대로 STOMP 로 전달한다.
    public void onMessage(byte[] body) {
        try {
            long roomId = extractRoomId(body);
            Message<byte[]> message = MessageBuilder.withPayload(body)
                    .setHeader(MessageHeaders.CONTENT_TYPE, MimeTypeUtils.APPLICATION_JSON)
                    .build();
            messagingTemplate.send("/sub/chat/room/" + roomId, message);
            log.debug("Redis Sub -> WebSocket Sent: roomId={}", roomId);
        } catch (Exception e) {
            log.error("Redis Chat Message Processing Error", e);
        }
    }

    private long extractRoomId(byte[] body) throws IOException {
        try (JsonParser parser = objectMapper.getFactory().createParser(body)) {
            while (parser.nextToken() != null) {
                if (parser.currentToken() == JsonToken.FIELD_NAME && "roomId".equals(parser.currentName())) {
                    parser.nextToken();
                    return parser.getLongValue();
                }
            }
        }
        throw new IllegalStateException("roomId not found in chat broadcast payload");
    }
}
