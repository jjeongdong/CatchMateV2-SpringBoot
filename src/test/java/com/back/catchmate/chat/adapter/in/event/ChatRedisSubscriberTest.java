package com.back.catchmate.chat.adapter.in.event;

import com.back.catchmate.chat.application.event.ChatMessageBroadcastEvent;
import com.back.catchmate.chat.domain.enums.MessageType;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.messaging.Message;
import org.springframework.messaging.simp.SimpMessagingTemplate;

import java.time.LocalDateTime;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.BDDMockito.then;

// ChatMessageRedisPublisher 가 발행할 때 쓰는 것과 동일한 방식(JavaTimeModule + ISO 문자열)으로
// ObjectMapper 를 구성해, "Redis 로 오는 JSON == STOMP 로 나가는 JSON" 이라는 전제가
// 실제로 바이트 단위로 성립하는지 고정한다.
@ExtendWith(MockitoExtension.class)
class ChatRedisSubscriberTest {

    private final ObjectMapper objectMapper = new ObjectMapper()
            .registerModule(new JavaTimeModule())
            .disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS);

    @Mock
    private SimpMessagingTemplate messagingTemplate;

    @Test
    @DisplayName("Redis 에서 받은 JSON 바이트를 역직렬화·재직렬화 없이 원본 그대로 STOMP 로 전달한다")
    void redis_에서_받은_JSON_을_원본_그대로_STOMP_로_전달한다() throws Exception {
        // given
        ChatRedisSubscriber sut = new ChatRedisSubscriber(objectMapper, messagingTemplate);
        ChatMessageBroadcastEvent event = ChatMessageBroadcastEvent.builder()
                .messageId(1L)
                .roomId(42L)
                .senderId(7L)
                .senderNickname("동훈")
                .senderProfileImage("https://example.com/profile.png")
                .content("안녕하세요")
                .messageType(MessageType.TEXT)
                .createdAt(LocalDateTime.of(2026, 7, 28, 12, 34, 56))
                .build();
        byte[] originalBytes = objectMapper.writeValueAsBytes(event);

        // when
        sut.onMessage(originalBytes);

        // then
        @SuppressWarnings("unchecked")
        ArgumentCaptor<Message<byte[]>> messageCaptor = ArgumentCaptor.forClass(Message.class);
        then(messagingTemplate).should().send(eq("/sub/chat/room/42"), messageCaptor.capture());
        assertThat(messageCaptor.getValue().getPayload()).isEqualTo(originalBytes);
    }
}
