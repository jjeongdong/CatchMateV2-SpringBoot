package com.back.catchmate.enroll.domain;

import static org.assertj.core.api.Assertions.assertThat;

import com.back.catchmate.enroll.domain.exception.EnrollAcceptConflictException;
import com.back.catchmate.enroll.domain.exception.EnrollAcceptInProgressException;
import com.back.catchmate.enroll.domain.exception.EnrollAlreadyAcceptedException;
import com.back.catchmate.enroll.domain.exception.EnrollAlreadyPendingException;
import com.back.catchmate.enroll.domain.exception.EnrollAlreadyRejectedException;
import com.back.catchmate.enroll.domain.exception.EnrollNotApplicantException;
import com.back.catchmate.enroll.domain.exception.EnrollNotBoardWriterException;
import com.back.catchmate.enroll.domain.exception.EnrollNotFoundException;
import com.back.catchmate.enroll.domain.exception.EnrollNotParticipantException;
import com.back.catchmate.enroll.domain.exception.EnrollSelfNotAllowedException;
import com.back.catchmate.global.error.BusinessException;
import com.back.catchmate.global.error.ErrorType;
import java.util.function.Supplier;
import java.util.stream.Stream;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

class EnrollExceptionTest {

    static Stream<Arguments> exceptions() {
        return Stream.of(
                Arguments.of(
                        (Supplier<BusinessException>) EnrollNotFoundException::new,
                        "ENROLL_NOT_FOUND",
                        ErrorType.NOT_FOUND),
                Arguments.of(
                        (Supplier<BusinessException>) EnrollSelfNotAllowedException::new,
                        "ENROLL_SELF_NOT_ALLOWED",
                        ErrorType.INVALID),
                Arguments.of(
                        (Supplier<BusinessException>) EnrollAlreadyPendingException::new,
                        "ENROLL_ALREADY_PENDING",
                        ErrorType.CONFLICT),
                Arguments.of(
                        (Supplier<BusinessException>) EnrollAlreadyRejectedException::new,
                        "ENROLL_ALREADY_REJECTED",
                        ErrorType.CONFLICT),
                Arguments.of(
                        (Supplier<BusinessException>) EnrollAlreadyAcceptedException::new,
                        "ENROLL_ALREADY_ACCEPTED",
                        ErrorType.CONFLICT),
                Arguments.of(
                        (Supplier<BusinessException>) EnrollNotBoardWriterException::new,
                        "ENROLL_NOT_BOARD_WRITER",
                        ErrorType.FORBIDDEN),
                Arguments.of(
                        (Supplier<BusinessException>) EnrollNotApplicantException::new,
                        "ENROLL_NOT_APPLICANT",
                        ErrorType.FORBIDDEN),
                Arguments.of(
                        (Supplier<BusinessException>) EnrollNotParticipantException::new,
                        "ENROLL_NOT_PARTICIPANT",
                        ErrorType.FORBIDDEN),
                Arguments.of(
                        (Supplier<BusinessException>) EnrollAcceptInProgressException::new,
                        "ENROLL_ACCEPT_IN_PROGRESS",
                        ErrorType.CONFLICT),
                Arguments.of(
                        (Supplier<BusinessException>) EnrollAcceptConflictException::new,
                        "ENROLL_ACCEPT_CONFLICT",
                        ErrorType.CONFLICT));
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
