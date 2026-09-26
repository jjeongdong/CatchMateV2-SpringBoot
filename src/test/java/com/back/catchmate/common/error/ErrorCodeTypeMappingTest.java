package com.back.catchmate.common.error;

import static org.assertj.core.api.Assertions.assertThat;

import com.back.catchmate.common.error.exception.BaseException;
import com.back.catchmate.global.error.BusinessException;
import com.back.catchmate.global.error.ErrorType;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

class ErrorCodeTypeMappingTest {

    @ParameterizedTest
    @CsvSource({
        "BLOCKED_USER_BOARD, INVALID",
        "INVALID_ACCESS_TOKEN, UNAUTHORIZED",
        "CHATROOM_READ_ONLY, FORBIDDEN",
        "BOARD_NOT_FOUND, NOT_FOUND",
        "ENROLL_ACCEPT_CONFLICT, CONFLICT",
        "NOTIFICATION_OUTBOX_SAVE_FAILED, INTERNAL"
    })
    @DisplayName("기존 HTTP 상태가 같은 의미의 ErrorType 으로 옮겨졌다")
    void mapsLegacyStatusToErrorType(ErrorCode errorCode, ErrorType expected) {
        assertThat(errorCode.type()).isEqualTo(expected);
    }

    @Test
    @DisplayName("기존 BaseException 도 BusinessException 으로 잡히고 옛 enum 을 그대로 돌려준다")
    void baseExceptionIsBusinessException() {
        // when
        BaseException exception = new BaseException(ErrorCode.BOARD_NOT_FOUND);

        // then
        assertThat(exception).isInstanceOf(BusinessException.class);
        assertThat(exception.getErrorCode()).isSameAs(ErrorCode.BOARD_NOT_FOUND);
        assertThat(exception.getMessage()).isEqualTo("존재하지 않는 게시글입니다.");
    }
}
