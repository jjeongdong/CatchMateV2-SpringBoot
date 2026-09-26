package com.back.catchmate.report.domain.exception;

import static org.assertj.core.api.Assertions.assertThat;

import com.back.catchmate.global.error.BusinessException;
import com.back.catchmate.global.error.ErrorType;
import com.back.catchmate.notice.domain.exception.NoticeNotFoundException;
import java.util.function.Supplier;
import java.util.stream.Stream;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

class NoticeReportExceptionsTest {

    static Stream<Arguments> exceptions() {
        return Stream.of(
                Arguments.of(
                        (Supplier<BusinessException>) NoticeNotFoundException::new,
                        "NOTICE_NOT_FOUND",
                        ErrorType.NOT_FOUND,
                        "존재하지 않는 공지입니다."),
                Arguments.of(
                        (Supplier<BusinessException>) ReportNotFoundException::new,
                        "REPORT_NOT_FOUND",
                        ErrorType.NOT_FOUND,
                        "존재하지 않는 신고입니다."),
                Arguments.of(
                        (Supplier<BusinessException>) ReportSelfNotAllowedException::new,
                        "REPORT_SELF_NOT_ALLOWED",
                        ErrorType.INVALID,
                        "자기 자신을 신고할 수 없습니다."));
    }

    @ParameterizedTest(name = "{1}")
    @MethodSource("exceptions")
    @DisplayName("예외마다 전용 코드·타입·메시지를 가진다")
    void carriesErrorCode(Supplier<BusinessException> factory, String code, ErrorType type, String message) {
        // when
        BusinessException exception = factory.get();

        // then
        assertThat(exception.getErrorCode().name()).isEqualTo(code);
        assertThat(exception.getErrorCode().type()).isEqualTo(type);
        assertThat(exception.getMessage()).isEqualTo(message);
    }
}
