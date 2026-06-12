package com.back.catchmate.chat.adapter.out;

import com.back.catchmate.chat.application.port.out.ChatMessageBufferPort;
import com.back.catchmate.chat.domain.enums.MessageType;
import com.back.catchmate.chat.domain.model.ChatMessage;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.script.DefaultRedisScript;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;

@Slf4j
@Component
@RequiredArgsConstructor
public class RedisChatMessageBufferAdapter implements ChatMessageBufferPort {
    private static final String BUFFER_KEY = "chat:message:buffer";
    private static final DateTimeFormatter FORMATTER = DateTimeFormatter.ISO_LOCAL_DATE_TIME;

    private final StringRedisTemplate redisTemplate;
    private final ObjectMapper objectMapper;

    private static final DefaultRedisScript<List> DRAIN_SCRIPT = new DefaultRedisScript<>(
            "local entries = redis.call('LRANGE', KEYS[1], 0, -1) " +
                    "if #entries > 0 then " +
                    "  redis.call('DEL', KEYS[1]) " +
                    "end " +
                    "return entries",
            List.class
    );

    @Override
    public void buffer(ChatMessage chatMessage) {
        try {
            String json = objectMapper.writeValueAsString(toBufferMap(chatMessage));
            redisTemplate.opsForList().leftPush(BUFFER_KEY, json);
        } catch (JsonProcessingException e) {
            log.error("채팅 메시지 버퍼링 실패 - roomId: {}, sequence: {}",
                    chatMessage.getChatRoomId(), chatMessage.getSequence(), e);
        }
    }

    @Override
    @SuppressWarnings("unchecked")
    public List<ChatMessage> drainAll() {
        List<String> entries = redisTemplate.execute(DRAIN_SCRIPT, Collections.singletonList(BUFFER_KEY));

        if (entries == null || entries.isEmpty()) {
            return List.of();
        }

        List<ChatMessage> messages = new ArrayList<>();
        for (String json : entries) {
            try {
                messages.add(fromJson(json));
            } catch (JsonProcessingException e) {
                log.error("채팅 메시지 버퍼 역직렬화 실패", e);
            }
        }
        return messages;
    }

    @Override
    public List<ChatMessage> readByRoomId(Long chatRoomId) {
        List<String> entries = redisTemplate.opsForList().range(BUFFER_KEY, 0, -1);

        if (entries == null || entries.isEmpty()) {
            return List.of();
        }

        List<ChatMessage> messages = new ArrayList<>();
        for (String json : entries) {
            try {
                Map<String, Object> map = objectMapper.readValue(json, Map.class);
                Long roomId = ((Number) map.get("chatRoomId")).longValue();
                if (roomId.equals(chatRoomId)) {
                    messages.add(fromJson(json));
                }
            } catch (JsonProcessingException e) {
                log.error("채팅 메시지 버퍼 읽기 실패", e);
            }
        }
        return messages;
    }

    private Map<String, Object> toBufferMap(ChatMessage chatMessage) {
        return Map.of(
                "chatRoomId", chatMessage.getChatRoomId(),
                "senderId", chatMessage.getSenderId(),
                "content", chatMessage.getContent(),
                "messageType", chatMessage.getMessageType().name(),
                "sequence", chatMessage.getSequence(),
                "createdAt", chatMessage.getCreatedAt().format(FORMATTER)
        );
    }

    @SuppressWarnings("unchecked")
    private ChatMessage fromJson(String json) throws JsonProcessingException {
        Map<String, Object> map = objectMapper.readValue(json, Map.class);

        Long chatRoomId = ((Number) map.get("chatRoomId")).longValue();
        Long senderId = ((Number) map.get("senderId")).longValue();
        String content = (String) map.get("content");
        MessageType messageType = MessageType.valueOf((String) map.get("messageType"));
        Long sequence = ((Number) map.get("sequence")).longValue();
        LocalDateTime createdAt = LocalDateTime.parse((String) map.get("createdAt"), FORMATTER);

        return ChatMessage.builder()
                .chatRoomId(chatRoomId)
                .senderId(senderId)
                .content(content)
                .messageType(messageType)
                .sequence(sequence)
                .createdAt(createdAt)
                .build();
    }
}
