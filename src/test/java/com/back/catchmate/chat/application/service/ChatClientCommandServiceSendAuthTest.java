package com.back.catchmate.chat.application.service;

import com.back.catchmate.chat.application.dto.command.ChatMessageCommand;
import com.back.catchmate.chat.application.port.out.external.UserFetchPort;
import com.back.catchmate.chat.domain.enums.MessageType;
import com.back.catchmate.common.error.ErrorCode;
import com.back.catchmate.common.error.exception.BaseException;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.BDDMockito.then;

/**
 * 클라이언트발 전송 경로의 타입 게이트 검증.
 * SYSTEM 은 입장·퇴장 시 ChatRoomService 가 직접 만드는 서버 전용 타입이라,
 * 클라이언트가 messageType 에 실어 보내면 시스템 메시지를 사칭할 수 있었다. 그 회귀를 막는다.
 */
@ExtendWith(MockitoExtension.class)
class ChatClientCommandServiceSendAuthTest {

    private static final Long ROOM_ID = 5L;
    private static final Long SENDER_ID = 1L;

    @Mock
    private ChatRoomService chatRoomService;

    @Mock
    private ChatMessageService chatMessageService;

    @Mock
    private ChatRoomMemberService chatRoomMemberService;

    @Mock
    private UserFetchPort userFetchPort;

    @InjectMocks
    private ChatClientCommandService sut;

    @Test
    @DisplayName("클라이언트가 SYSTEM 타입으로 보내면 BAD_REQUEST 로 차단하고 저장·발행하지 않는다")
    void 클라이언트발_SYSTEM_메시지는_차단된다() {
        // given
        ChatMessageCommand command = new ChatMessageCommand(
                ROOM_ID, SENDER_ID, "○○님이 입장하셨습니다.", MessageType.SYSTEM);

        // when & then
        assertThatThrownBy(() -> sut.sendMessage(SENDER_ID, command))
                .isInstanceOf(BaseException.class)
                .satisfies(e -> assertThat(((BaseException) e).getErrorCode())
                        .isEqualTo(ErrorCode.BAD_REQUEST));

        // 타입 게이트는 그 어떤 부수효과보다 먼저 걸려야 한다 (조회·시퀀스·저장·브로드캐스트 전부 없음)
        then(userFetchPort).shouldHaveNoInteractions();
        then(chatMessageService).shouldHaveNoInteractions();
    }
}
