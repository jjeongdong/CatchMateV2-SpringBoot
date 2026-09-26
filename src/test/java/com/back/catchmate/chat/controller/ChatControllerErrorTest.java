package com.back.catchmate.chat.controller;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;

import com.back.catchmate.chat.dto.request.ChatMessageRequest;
import com.back.catchmate.chat.dto.response.ChatErrorResponse;
import com.back.catchmate.chat.entity.MessageType;
import com.back.catchmate.chat.service.ChatCommandService;
import com.back.catchmate.common.error.exception.BaseException;
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

class ChatControllerErrorTest {

    private static final Long CHAT_ROOM_ID = 10L;

    private final ChatController chatController = new ChatController(mock(ChatCommandService.class));
    private final ChatMessageRequest request = new ChatMessageRequest(CHAT_ROOM_ID, "안녕하세요", MessageType.TEXT);

    @Test
    @DisplayName("BusinessException 전체가 도메인 에러 핸들러로 라우팅된다")
    void routesBusinessExceptionToDomainHandler() throws Exception {
        // given
        Method handler = ChatController.class.getMethod(
                "handleBusinessException", BusinessException.class, ChatMessageRequest.class);

        // when
        MessageExceptionHandler annotation = handler.getAnnotation(MessageExceptionHandler.class);

        // then
        assertThat(annotation.value()).containsExactly(BusinessException.class);
    }

    @Test
    @DisplayName("도메인 오류는 재전송해도 같으므로 retryable=false 로 통보한다")
    void domainErrorIsNotRetryable() {
        // when
        ChatErrorResponse response =
                chatController.handleBusinessException(new TestException(TestErrorCode.TEST_FORBIDDEN), request);

        // then
        assertThat(response).isEqualTo(new ChatErrorResponse(CHAT_ROOM_ID, "TEST_FORBIDDEN", "읽기 전용입니다.", false));
    }

    @Test
    @DisplayName("외부 장애는 재전송이 유효하므로 retryable=true 로 통보한다")
    void externalErrorIsRetryable() {
        // when
        ChatErrorResponse response =
                chatController.handleBusinessException(new TestException(TestErrorCode.TEST_EXTERNAL), request);

        // then
        assertThat(response.retryable()).isTrue();
        assertThat(response.code()).isEqualTo("TEST_EXTERNAL");
    }

    @Test
    @DisplayName("기존 BaseException 도 같은 핸들러에서 기존 코드로 통보한다")
    void legacyBaseExceptionKeepsItsCode() {
        // when
        ChatErrorResponse response = chatController.handleBusinessException(
                new BaseException(com.back.catchmate.common.error.ErrorCode.CHATROOM_READ_ONLY), request);

        // then
        assertThat(response.code()).isEqualTo("CHATROOM_READ_ONLY");
        assertThat(response.retryable()).isFalse();
    }

    @Test
    @DisplayName("검증 실패는 INVALID_INPUT 으로 통보한다")
    void validationFailureIsInvalidInput() throws Exception {
        // given
        MethodParameter parameter = new MethodParameter(
                ChatController.class.getMethod("sendMessage", ChatMessageRequest.class, Principal.class), 0);
        MethodArgumentNotValidException exception = new MethodArgumentNotValidException(
                MessageBuilder.withPayload("{}").build(), parameter);

        // when
        ChatErrorResponse response = chatController.handleValidationException(exception, request);

        // then
        assertThat(response).isEqualTo(new ChatErrorResponse(CHAT_ROOM_ID, "INVALID_INPUT", "입력값이 올바르지 않습니다.", false));
    }

    @Test
    @DisplayName("예기치 못한 오류는 INTERNAL_SERVER_ERROR 와 retryable=true 로 통보한다")
    void unexpectedErrorIsRetryableInternalServerError() {
        // when
        ChatErrorResponse response = chatController.handleUnexpectedException(new IllegalStateException("실패"));

        // then
        assertThat(response).isEqualTo(new ChatErrorResponse(null, "INTERNAL_SERVER_ERROR", "서버 오류입니다.", true));
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
