package com.back.catchmate.chat.presentation;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.BDDMockito.then;

import com.back.catchmate.chat.application.ChatQueryService;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class ChatRoomSubscriptionAuthorizerTest {

    @Mock
    private ChatQueryService chatQueryService;

    @InjectMocks
    private ChatRoomSubscriptionAuthorizer sut;

    @Test
    @DisplayName("채팅방 구독 주소만 맡는다")
    void supportsOnlyChatRoomDestination() {
        assertThat(sut.supports("/sub/chat/room/5")).isTrue();
        assertThat(sut.supports("/user/queue/errors")).isFalse();
        assertThat(sut.supports("/sub/chat/15")).isFalse();
    }

    @Test
    @DisplayName("주소의 방 번호 부분을 넘겨 구독을 검사한다")
    void delegatesRoomPart() {
        // when
        sut.authorize(1L, "/sub/chat/room/5");

        // then
        then(chatQueryService).should().verifySubscription(1L, "5");
    }
}
