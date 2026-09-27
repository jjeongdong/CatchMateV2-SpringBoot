package com.back.catchmate.notification.domain;

import static org.assertj.core.api.Assertions.assertThat;

import com.back.catchmate.global.error.BusinessException;
import com.back.catchmate.global.error.ErrorType;
import com.back.catchmate.notification.domain.exception.NotificationCursorInvalidException;
import com.back.catchmate.notification.domain.exception.NotificationNotFoundException;
import com.back.catchmate.notification.domain.exception.NotificationNotOwnerException;
import com.back.catchmate.notification.domain.exception.NotificationOutboxSaveFailedException;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

class NotificationExceptionTest {

    @Test
    @DisplayName("예외마다 전용 코드와 타입을 가진다")
    void codes() {
        assertCode(
                new NotificationNotFoundException(), NotificationErrorCode.NOTIFICATION_NOT_FOUND, ErrorType.NOT_FOUND);
        assertCode(
                new NotificationNotOwnerException(), NotificationErrorCode.NOTIFICATION_NOT_OWNER, ErrorType.FORBIDDEN);
        assertCode(
                new NotificationCursorInvalidException(),
                NotificationErrorCode.NOTIFICATION_CURSOR_INVALID,
                ErrorType.INVALID);
        assertCode(
                new NotificationOutboxSaveFailedException(new IllegalStateException("x")),
                NotificationErrorCode.NOTIFICATION_OUTBOX_SAVE_FAILED,
                ErrorType.INTERNAL);
    }

    @Test
    @DisplayName("아웃박스 저장 실패는 원인을 잃지 않는다")
    void keepsCause() {
        IllegalStateException cause = new IllegalStateException("직렬화 실패");

        assertThat(new NotificationOutboxSaveFailedException(cause)).hasCause(cause);
    }

    private static void assertCode(BusinessException exception, NotificationErrorCode code, ErrorType type) {
        assertThat(exception.getErrorCode()).isEqualTo(code);
        assertThat(code.type()).isEqualTo(type);
        assertThat(exception.getMessage()).isEqualTo(code.message());
    }
}
