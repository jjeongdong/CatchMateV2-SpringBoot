package com.back.catchmate.chat.application.service;

import com.back.catchmate.chat.application.port.out.ChatMembershipCachePort;
import com.back.catchmate.chat.application.port.out.ChatMembershipCachePort.MembershipSnapshot;
import com.back.catchmate.chat.application.port.out.ChatSequencePort;
import com.back.catchmate.chat.application.port.out.persistence.ChatRoomMemberRepository;
import com.back.catchmate.chat.domain.enums.MessageType;
import com.back.catchmate.common.error.ErrorCode;
import com.back.catchmate.common.error.exception.BaseException;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.never;
import static org.mockito.BDDMockito.then;

/**
 * prepareSequence 의 인가 게이트 검증.
 * 멤버십 검사가 messageType 분기 안에 있던 시절, TEXT 가 아닌 메시지는 검사를 건너뛰어
 * 참여하지 않은 방으로도 전송이 가능했다. 그 회귀를 막는다.
 */
@ExtendWith(MockitoExtension.class)
class ChatMessageServiceSequenceAuthTest {

    private static final Long ROOM_ID = 5L;
    private static final Long SENDER_ID = 1L;

    @Mock
    private ChatMembershipCachePort chatMembershipCachePort;

    @Mock
    private ChatRoomMemberRepository chatRoomMemberRepository;

    @Mock
    private ChatSequencePort chatSequencePort;

    @InjectMocks
    private ChatMessageService sut;

    @ParameterizedTest(name = "{0}")
    @EnumSource(MessageType.class)
    @DisplayName("멤버가 아니면 메시지 타입과 무관하게 시퀀스를 발급하지 않는다")
    void 멤버가_아니면_모든_타입에서_차단된다(MessageType messageType) {
        // given
        given(chatMembershipCachePort.find(ROOM_ID, SENDER_ID)).willReturn(Optional.empty());
        given(chatRoomMemberRepository.findByChatRoomIdAndUserId(ROOM_ID, SENDER_ID))
                .willReturn(Optional.empty());

        // when & then
        assertThatThrownBy(() -> sut.prepareSequence(ROOM_ID, SENDER_ID, messageType))
                .isInstanceOf(BaseException.class)
                .satisfies(e -> assertThat(((BaseException) e).getErrorCode())
                        .isEqualTo(ErrorCode.CHATROOM_MEMBER_NOT_FOUND));

        then(chatSequencePort).shouldHaveNoInteractions();
    }

    @ParameterizedTest(name = "{0}")
    @EnumSource(MessageType.class)
    @DisplayName("퇴장한 멤버는 메시지 타입과 무관하게 시퀀스를 발급받지 못한다")
    void 퇴장한_멤버는_모든_타입에서_차단된다(MessageType messageType) {
        // given
        given(chatMembershipCachePort.find(ROOM_ID, SENDER_ID))
                .willReturn(Optional.of(new MembershipSnapshot(false, false)));

        // when & then
        assertThatThrownBy(() -> sut.prepareSequence(ROOM_ID, SENDER_ID, messageType))
                .isInstanceOf(BaseException.class)
                .satisfies(e -> assertThat(((BaseException) e).getErrorCode())
                        .isEqualTo(ErrorCode.CHATROOM_MEMBER_NOT_FOUND));

        then(chatSequencePort).shouldHaveNoInteractions();
    }

    @ParameterizedTest(name = "{0}")
    @EnumSource(MessageType.class)
    @DisplayName("읽기 전용 멤버는 메시지 타입과 무관하게 시퀀스를 발급받지 못한다")
    void 읽기전용_멤버는_모든_타입에서_차단된다(MessageType messageType) {
        // given
        given(chatMembershipCachePort.find(ROOM_ID, SENDER_ID))
                .willReturn(Optional.of(new MembershipSnapshot(true, true)));

        // when & then
        assertThatThrownBy(() -> sut.prepareSequence(ROOM_ID, SENDER_ID, messageType))
                .isInstanceOf(BaseException.class)
                .satisfies(e -> assertThat(((BaseException) e).getErrorCode())
                        .isEqualTo(ErrorCode.CHATROOM_READ_ONLY));

        then(chatSequencePort).shouldHaveNoInteractions();
    }

    @Test
    @DisplayName("정상 멤버의 TEXT 메시지는 새 시퀀스를 INCR 로 발급받는다")
    void 정상멤버의_TEXT는_시퀀스를_증가시킨다() {
        // given
        given(chatMembershipCachePort.find(ROOM_ID, SENDER_ID))
                .willReturn(Optional.of(new MembershipSnapshot(true, false)));
        given(chatSequencePort.generateSequence(ROOM_ID)).willReturn(42L);

        // when
        Long sequence = sut.prepareSequence(ROOM_ID, SENDER_ID, MessageType.TEXT);

        // then
        assertThat(sequence).isEqualTo(42L);
        then(chatSequencePort).should(never()).getCurrentSequence(anyLong());
    }
}
