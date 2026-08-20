package com.back.catchmate.global.config.security;

import com.back.catchmate.auth.application.port.in.AuthInternalQueryUseCase;
import com.back.catchmate.chat.application.port.in.ChatClientQueryUseCase;
import com.back.catchmate.common.error.ErrorCode;
import com.back.catchmate.common.error.exception.BaseException;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.messaging.Message;
import org.springframework.messaging.MessageChannel;
import org.springframework.messaging.simp.stomp.StompCommand;
import org.springframework.messaging.simp.stomp.StompHeaderAccessor;
import org.springframework.messaging.support.MessageBuilder;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.util.AntPathMatcher;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.then;

@ExtendWith(MockitoExtension.class)
class StompAuthChannelInterceptorTest {

    private static final Long USER_ID = 1L;
    private static final Long ROOM_ID = 5L;
    private static final String CHAT_ROOM_DESTINATION = "/sub/chat/room/5";
    private static final String NOTIFICATION_DESTINATION = "/user/queue/notifications";

    @Mock
    private AuthInternalQueryUseCase authInternalQueryUseCase;

    @Mock
    private ChatClientQueryUseCase chatClientQueryUseCase;

    @Mock
    private MessageChannel channel;

    @InjectMocks
    private StompAuthChannelInterceptor sut;

    /**
     * 이 테스트가 존재하는 이유를 못 박는다.
     * 브로커가 구독 목적지를 정확히 일치로만 비교했다면 화이트리스트가 필요 없지만,
     * 실제로는 AntPathMatcher 패턴으로 매칭하므로 와일드카드 구독 하나가 전체 채팅방에 걸린다.
     */
    @Test
    @DisplayName("브로커의 기본 매처는 와일드카드 구독을 실제 채팅방·알림 목적지에 매칭시킨다")
    void 브로커_기본매처는_와일드카드를_실제_목적지에_매칭한다() {
        // given
        AntPathMatcher pathMatcher = new AntPathMatcher(); // DefaultSubscriptionRegistry 의 기본값

        // when & then
        assertThat(pathMatcher.isPattern("/sub/**")).isTrue();
        assertThat(pathMatcher.match("/sub/**", CHAT_ROOM_DESTINATION)).isTrue();
        assertThat(pathMatcher.match("/queue/**", "/queue/notifications-user7f3a")).isTrue();
    }

    @ParameterizedTest(name = "{0}")
    @ValueSource(strings = {
            "/sub/**",
            "/sub/chat/**",
            "/sub/chat/*/5",
            "/sub/chat/room/*",
            "/**",
            "/queue/**",
            "/queue/notifications-user7f3a",
            "/sub/chat/15"
    })
    @DisplayName("허용 목적지가 아닌 구독은 BAD_REQUEST 로 차단하고 참가 여부를 조회하지 않는다")
    void 허용되지_않은_목적지_구독은_차단된다(String destination) {
        // given
        Message<byte[]> message = subscribeMessage(destination, authenticated());

        // when & then
        assertThatThrownBy(() -> sut.preSend(message, channel))
                .isInstanceOf(BaseException.class)
                .satisfies(e -> assertThat(((BaseException) e).getErrorCode())
                        .isEqualTo(ErrorCode.BAD_REQUEST));

        then(chatClientQueryUseCase).shouldHaveNoInteractions();
    }

    @Test
    @DisplayName("목적지가 없는 SUBSCRIBE 는 BAD_REQUEST 로 차단한다")
    void 목적지가_없으면_차단된다() {
        // given
        Message<byte[]> message = subscribeMessage(null, authenticated());

        // when & then
        assertThatThrownBy(() -> sut.preSend(message, channel))
                .isInstanceOf(BaseException.class)
                .satisfies(e -> assertThat(((BaseException) e).getErrorCode())
                        .isEqualTo(ErrorCode.BAD_REQUEST));
    }

    @Test
    @DisplayName("참여 중인 채팅방 구독은 통과한다")
    void 참여중인_채팅방_구독은_통과한다() {
        // given
        given(chatClientQueryUseCase.canAccessChatRoom(USER_ID, ROOM_ID)).willReturn(true);
        Message<byte[]> message = subscribeMessage(CHAT_ROOM_DESTINATION, authenticated());

        // when
        Message<?> result = sut.preSend(message, channel);

        // then
        assertThat(result).isSameAs(message);
    }

    @Test
    @DisplayName("참여하지 않은 채팅방 구독은 USER_CHATROOM_NOT_FOUND 로 차단한다")
    void 미참여_채팅방_구독은_차단된다() {
        // given
        given(chatClientQueryUseCase.canAccessChatRoom(USER_ID, ROOM_ID)).willReturn(false);
        Message<byte[]> message = subscribeMessage(CHAT_ROOM_DESTINATION, authenticated());

        // when & then
        assertThatThrownBy(() -> sut.preSend(message, channel))
                .isInstanceOf(BaseException.class)
                .satisfies(e -> assertThat(((BaseException) e).getErrorCode())
                        .isEqualTo(ErrorCode.USER_CHATROOM_NOT_FOUND));
    }

    @Test
    @DisplayName("알림 큐 구독은 참가 검사 없이 통과한다")
    void 알림_큐_구독은_통과한다() {
        // given
        Message<byte[]> message = subscribeMessage(NOTIFICATION_DESTINATION, authenticated());

        // when
        Message<?> result = sut.preSend(message, channel);

        // then
        assertThat(result).isSameAs(message);
        then(chatClientQueryUseCase).shouldHaveNoInteractions();
    }

    @Test
    @DisplayName("인증되지 않은 채팅방 구독은 SOCKET_CONNECT_FAILED 로 차단한다")
    void 인증되지_않은_채팅방_구독은_차단된다() {
        // given
        Message<byte[]> message = subscribeMessage(CHAT_ROOM_DESTINATION, null);

        // when & then
        assertThatThrownBy(() -> sut.preSend(message, channel))
                .isInstanceOf(BaseException.class)
                .satisfies(e -> assertThat(((BaseException) e).getErrorCode())
                        .isEqualTo(ErrorCode.SOCKET_CONNECT_FAILED));
    }

    private Message<byte[]> subscribeMessage(String destination, Authentication user) {
        StompHeaderAccessor accessor = StompHeaderAccessor.create(StompCommand.SUBSCRIBE);
        accessor.setSessionId("session-1");
        accessor.setSubscriptionId("sub-0");
        if (destination != null) {
            accessor.setDestination(destination);
        }
        accessor.setUser(user);
        return MessageBuilder.createMessage(new byte[0], accessor.getMessageHeaders());
    }

    private Authentication authenticated() {
        return new UsernamePasswordAuthenticationToken(
                USER_ID, null, List.of(new SimpleGrantedAuthority("ROLE_USER")));
    }
}
