package com.back.catchmate.chat.application;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.then;
import static org.mockito.BDDMockito.willAnswer;
import static org.mockito.Mockito.times;

import com.back.catchmate.chat.domain.ChatMessage;
import com.back.catchmate.chat.domain.ChatMessageRepository;
import com.back.catchmate.chat.domain.ChatRoomRepository;
import com.back.catchmate.chat.domain.event.ChatMessageBroadcastEvent;
import com.back.catchmate.chat.domain.event.ChatMessageSentEvent;
import com.back.catchmate.chat.fixture.ChatFixture;
import com.back.catchmate.user.application.UserQueryApi;
import java.util.List;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.test.util.ReflectionTestUtils;

@ExtendWith(MockitoExtension.class)
class ChatMessageWriterTest {

    @Mock
    private ChatRoomRepository chatRoomRepository;

    @Mock
    private ChatMessageRepository chatMessageRepository;

    @Mock
    private UserQueryApi userQueryApi;

    @Mock
    private ApplicationEventPublisher eventPublisher;

    @InjectMocks
    private ChatMessageWriter chatMessageWriter;

    @Test
    @DisplayName("방을 SELECT 하지 않는 참조로 저장하고, 방송·알림 이벤트 두 개를 같은 트랜잭션에서 발행한다")
    void writeText() {
        // given
        given(chatRoomRepository.getReference(5L)).willReturn(ChatFixture.room(5L, 10L, 0));
        willAnswer(invocation -> {
                    ChatMessage message = invocation.getArgument(0);
                    ReflectionTestUtils.setField(message, "id", 100L);
                    return message;
                })
                .given(chatMessageRepository)
                .save(any(ChatMessage.class));
        given(userQueryApi.getInfo(1L)).willReturn(ChatCommandServiceTest.user(1L, "철수"));

        // when
        chatMessageWriter.writeText(5L, 1L, "안녕", 42L);

        // then
        ArgumentCaptor<Object> events = ArgumentCaptor.forClass(Object.class);
        then(eventPublisher).should(times(2)).publishEvent(events.capture());
        List<Object> published = events.getAllValues();
        assertThat(published.get(0)).isInstanceOfSatisfying(ChatMessageBroadcastEvent.class, event -> {
            assertThat(event.roomId()).isEqualTo(5L);
            assertThat(event.senderNickname()).isEqualTo("철수");
        });
        assertThat(published.get(1)).isEqualTo(new ChatMessageSentEvent(5L, 100L, 1L, "안녕"));
    }
}
