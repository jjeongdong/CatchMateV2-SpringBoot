package com.back.catchmate.chat.domain;

import static org.assertj.core.api.Assertions.assertThat;

import com.back.catchmate.chat.domain.exception.ChatCursorInvalidException;
import com.back.catchmate.chat.domain.exception.ChatMessageTypeNotAllowedException;
import com.back.catchmate.chat.domain.exception.ChatRoomMemberNotFoundException;
import com.back.catchmate.chat.domain.exception.ChatRoomNotFoundException;
import com.back.catchmate.chat.domain.exception.ChatRoomNotHostException;
import com.back.catchmate.chat.domain.exception.ChatRoomReadOnlyException;
import com.back.catchmate.chat.domain.exception.ChatRoomReentryNotAllowedException;
import com.back.catchmate.chat.domain.exception.ChatSelfKickNotAllowedException;
import com.back.catchmate.chat.domain.exception.ChatSenderUnauthenticatedException;
import com.back.catchmate.chat.domain.exception.ChatSubscriptionDestinationInvalidException;
import com.back.catchmate.global.error.BusinessException;
import com.back.catchmate.global.error.ErrorType;
import java.util.function.Supplier;
import java.util.stream.Stream;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

class ChatExceptionTest {

    static Stream<Arguments> exceptions() {
        return Stream.of(
                Arguments.of(
                        (Supplier<BusinessException>) ChatRoomNotFoundException::new,
                        "CHAT_ROOM_NOT_FOUND",
                        ErrorType.NOT_FOUND),
                Arguments.of(
                        (Supplier<BusinessException>) ChatRoomMemberNotFoundException::new,
                        "CHAT_ROOM_MEMBER_NOT_FOUND",
                        ErrorType.NOT_FOUND),
                Arguments.of(
                        (Supplier<BusinessException>) ChatRoomReentryNotAllowedException::new,
                        "CHAT_ROOM_REENTRY_NOT_ALLOWED",
                        ErrorType.CONFLICT),
                Arguments.of(
                        (Supplier<BusinessException>) ChatRoomReadOnlyException::new,
                        "CHAT_ROOM_READ_ONLY",
                        ErrorType.FORBIDDEN),
                Arguments.of(
                        (Supplier<BusinessException>) ChatRoomNotHostException::new,
                        "CHAT_ROOM_NOT_HOST",
                        ErrorType.FORBIDDEN),
                Arguments.of(
                        (Supplier<BusinessException>) ChatSelfKickNotAllowedException::new,
                        "CHAT_SELF_KICK_NOT_ALLOWED",
                        ErrorType.INVALID),
                Arguments.of(
                        (Supplier<BusinessException>) ChatMessageTypeNotAllowedException::new,
                        "CHAT_MESSAGE_TYPE_NOT_ALLOWED",
                        ErrorType.INVALID),
                Arguments.of(
                        (Supplier<BusinessException>) ChatSenderUnauthenticatedException::new,
                        "CHAT_SENDER_UNAUTHENTICATED",
                        ErrorType.UNAUTHORIZED),
                Arguments.of(
                        (Supplier<BusinessException>) ChatSubscriptionDestinationInvalidException::new,
                        "CHAT_SUBSCRIPTION_DESTINATION_INVALID",
                        ErrorType.INVALID),
                Arguments.of(
                        (Supplier<BusinessException>) ChatCursorInvalidException::new,
                        "CHAT_CURSOR_INVALID",
                        ErrorType.INVALID));
    }

    @ParameterizedTest(name = "{1}")
    @MethodSource("exceptions")
    @DisplayName("예외마다 전용 코드와 타입을 가진다")
    void mapsToErrorCode(Supplier<BusinessException> factory, String code, ErrorType type) {
        BusinessException exception = factory.get();

        assertThat(exception.getErrorCode().name()).isEqualTo(code);
        assertThat(exception.getErrorCode().type()).isEqualTo(type);
        assertThat(exception.getMessage()).isNotBlank();
    }
}
