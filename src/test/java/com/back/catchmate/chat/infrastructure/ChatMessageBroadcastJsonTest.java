package com.back.catchmate.chat.infrastructure;

import static org.assertj.core.api.Assertions.assertThat;

import com.back.catchmate.chat.domain.MessageType;
import com.back.catchmate.chat.domain.event.ChatMessageBroadcastEvent;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.time.LocalDateTime;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

// 서버끼리 Redis 로 주고받는 JSON 이다. 롤링 배포 중 옛 서버와 새 서버가 섞여도 통하도록 모양을 못 박는다.
class ChatMessageBroadcastJsonTest {

    private final ObjectMapper objectMapper = ChatRedisConfig.pubSubObjectMapper();

    @Test
    @DisplayName("옛 방송 JSON 과 필드 이름·날짜 형식이 같고 되읽을 수 있다")
    void serializesLegacyJson() throws Exception {
        // given
        ChatMessageBroadcastEvent event = new ChatMessageBroadcastEvent(
                1L,
                42L,
                7L,
                "동훈",
                "https://example.com/p.png",
                "안녕하세요",
                MessageType.TEXT,
                LocalDateTime.of(2026, 7, 28, 12, 34, 56));

        // when
        String json = objectMapper.writeValueAsString(event);

        // then
        assertThat(json)
                .isEqualTo("{\"messageId\":1,\"roomId\":42,\"senderId\":7,\"senderNickname\":\"동훈\","
                        + "\"senderProfileImage\":\"https://example.com/p.png\",\"content\":\"안녕하세요\","
                        + "\"messageType\":\"TEXT\",\"createdAt\":\"2026-07-28 12:34:56\"}");
        assertThat(objectMapper.readValue(json, ChatMessageBroadcastEvent.class))
                .isEqualTo(event);
    }
}
