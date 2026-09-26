package com.back.catchmate.chat.presentation;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;

import com.back.catchmate.chat.application.ChatCommandService;
import com.back.catchmate.chat.application.dto.result.ChatErrorResult;
import com.back.catchmate.chat.domain.MessageType;
import com.back.catchmate.chat.domain.exception.ChatRoomReadOnlyException;
import com.back.catchmate.chat.presentation.dto.request.ChatMessageSendRequest;
import com.back.catchmate.global.error.BusinessException;
import com.back.catchmate.global.error.ErrorCode;
import com.back.catchmate.global.error.ErrorType;
import java.lang.reflect.Method;
import java.security.Principal;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.core.MethodParameter;
import org.springframework.messaging.handler.annotation.MessageExceptionHandler;
import org.springframework.messaging.handler.annotation.support.MethodArgumentNotValidException;
import org.springframework.messaging.support.MessageBuilder;

class ChatStompControllerErrorTest {

    private static final Long CHAT_ROOM_ID = 10L;

    private final ChatStompController chatController = new ChatStompController(mock(ChatCommandService.class));
    private final ChatMessageSendRequest request = new ChatMessageSendRequest(CHAT_ROOM_ID, "안녕하세요", MessageType.TEXT);

    @Test
    @DisplayName("BusinessException 전체가 도메인 에러 핸들러로 라우팅된다")
    void routesBusinessExceptionToDomainHandler() throws Exception {
        // given
        Method handler = ChatStompController.class.getMethod(
                "handleBusinessException", BusinessException.class, ChatMessageSendRequest.class);

        // when
        MessageExceptionHandler annotation = handler.getAnnotation(MessageExceptionHandler.class);

        // then
        assertThat(annotation.value()).containsExactly(BusinessException.class);
    }

    @Test
    @DisplayName("도메인 오류는 재전송해도 같으므로 retryable=false 로 통보한다")
    void domainErrorIsNotRetryable() {
        // when
        ChatErrorResult response =
                chatController.handleBusinessException(new TestException(TestErrorCode.TEST_FORBIDDEN), request);

        // then
        assertThat(response).isEqualTo(new ChatErrorResult(CHAT_ROOM_ID, "TEST_FORBIDDEN", "읽기 전용입니다.", false));
    }

    @Test
    @DisplayName("외부 장애는 재전송이 유효하므로 retryable=true 로 통보한다")
    void externalErrorIsRetryable() {
        // when
        ChatErrorResult response =
                chatController.handleBusinessException(new TestException(TestErrorCode.TEST_EXTERNAL), request);

        // then
        assertThat(response.retryable()).isTrue();
        assertThat(response.code()).isEqualTo("TEST_EXTERNAL");
    }

    @Test
    @DisplayName("채팅 도메인 예외는 ChatErrorCode 이름으로 통보한다")
    void chatExceptionUsesChatErrorCode() {
        // when
        ChatErrorResult response = chatController.handleBusinessException(new ChatRoomReadOnlyException(), request);

        // then
        assertThat(response.code()).isEqualTo("CHAT_ROOM_READ_ONLY");
        assertThat(response.retryable()).isFalse();
    }

    @Test
    @DisplayName("검증 실패는 INVALID_INPUT 으로 통보한다")
    void validationFailureIsInvalidInput() throws Exception {
        // given
        MethodParameter parameter = new MethodParameter(
                ChatStompController.class.getMethod("sendMessage", ChatMessageSendRequest.class, Principal.class), 0);
        MethodArgumentNotValidException exception = new MethodArgumentNotValidException(
                MessageBuilder.withPayload("{}").build(), parameter);

        // when
        ChatErrorResult response = chatController.handleValidationException(exception, request);

        // then
        assertThat(response).isEqualTo(new ChatErrorResult(CHAT_ROOM_ID, "INVALID_INPUT", "입력값이 올바르지 않습니다.", false));
    }

    @Test
    @DisplayName("예기치 못한 오류는 INTERNAL_SERVER_ERROR 와 retryable=true 로 통보한다")
    void unexpectedErrorIsRetryableInternalServerError() {
        // when
        ChatErrorResult response = chatController.handleUnexpectedException(new IllegalStateException("실패"));

        // then
        assertThat(response).isEqualTo(new ChatErrorResult(null, "INTERNAL_SERVER_ERROR", "서버 오류입니다.", true));
    }

    enum TestErrorCode implements ErrorCode {
        TEST_FORBIDDEN(ErrorType.FORBIDDEN, "읽기 전용입니다."),
        TEST_EXTERNAL(ErrorType.EXTERNAL, "외부 서비스 장애입니다.");

        private final ErrorType type;
        private final String message;

        TestErrorCode(ErrorType type, String message) {
            this.type = type;
            this.message = message;
        }

        @Override
        public ErrorType type() {
            return type;
        }

        @Override
        public String message() {
            return message;
        }
    }

    static class TestException extends BusinessException {
        TestException(ErrorCode errorCode) {
            super(errorCode);
        }
    }
}
