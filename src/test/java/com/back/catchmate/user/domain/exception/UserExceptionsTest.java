package com.back.catchmate.user.domain.exception;

import static org.assertj.core.api.Assertions.assertThat;

import com.back.catchmate.global.error.BusinessException;
import com.back.catchmate.global.error.ErrorType;
import java.util.function.Supplier;
import java.util.stream.Stream;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

class UserExceptionsTest {

    static Stream<Arguments> exceptions() {
        return Stream.of(
                Arguments.of(
                        (Supplier<BusinessException>) UserNotFoundException::new,
                        "USER_NOT_FOUND",
                        ErrorType.NOT_FOUND,
                        "존재하지 않는 사용자입니다."),
                Arguments.of(
                        (Supplier<BusinessException>) UserAlreadyExistsException::new,
                        "USER_ALREADY_EXISTS",
                        ErrorType.CONFLICT,
                        "이미 가입된 사용자입니다."),
                Arguments.of(
                        (Supplier<BusinessException>) BlockNotFoundException::new,
                        "BLOCK_NOT_FOUND",
                        ErrorType.NOT_FOUND,
                        "존재하지 않는 차단 내역입니다."),
                Arguments.of(
                        (Supplier<BusinessException>) BlockAlreadyExistsException::new,
                        "BLOCK_ALREADY_EXISTS",
                        ErrorType.CONFLICT,
                        "해당 유저를 이미 차단했습니다."),
                Arguments.of(
                        (Supplier<BusinessException>) BlockSelfNotAllowedException::new,
                        "BLOCK_SELF_NOT_ALLOWED",
                        ErrorType.INVALID,
                        "자기 자신을 차단할 수 없습니다."));
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
