package com.back.catchmate.board.domain;

import static org.assertj.core.api.Assertions.assertThat;

import com.back.catchmate.board.domain.exception.BoardCheerClubMissingException;
import com.back.catchmate.board.domain.exception.BoardContentMissingException;
import com.back.catchmate.board.domain.exception.BoardCursorInvalidException;
import com.back.catchmate.board.domain.exception.BoardFullException;
import com.back.catchmate.board.domain.exception.BoardGameMissingException;
import com.back.catchmate.board.domain.exception.BoardNotEditableAfterEnrollException;
import com.back.catchmate.board.domain.exception.BoardNotFoundException;
import com.back.catchmate.board.domain.exception.BoardNotWriterException;
import com.back.catchmate.board.domain.exception.BoardTitleMissingException;
import com.back.catchmate.board.domain.exception.BoardWriterBlockedException;
import com.back.catchmate.global.error.BusinessException;
import com.back.catchmate.global.error.ErrorType;
import java.util.function.Supplier;
import java.util.stream.Stream;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

class BoardExceptionTest {

    static Stream<Arguments> exceptions() {
        return Stream.of(
                Arguments.of(
                        (Supplier<BusinessException>) BoardNotFoundException::new,
                        "BOARD_NOT_FOUND",
                        ErrorType.NOT_FOUND),
                Arguments.of(
                        (Supplier<BusinessException>) BoardNotWriterException::new,
                        "BOARD_NOT_WRITER",
                        ErrorType.FORBIDDEN),
                Arguments.of((Supplier<BusinessException>) BoardFullException::new, "BOARD_FULL", ErrorType.CONFLICT),
                Arguments.of(
                        (Supplier<BusinessException>) BoardNotEditableAfterEnrollException::new,
                        "BOARD_NOT_EDITABLE_AFTER_ENROLL",
                        ErrorType.CONFLICT),
                Arguments.of(
                        (Supplier<BusinessException>) BoardTitleMissingException::new,
                        "BOARD_TITLE_MISSING",
                        ErrorType.INVALID),
                Arguments.of(
                        (Supplier<BusinessException>) BoardContentMissingException::new,
                        "BOARD_CONTENT_MISSING",
                        ErrorType.INVALID),
                Arguments.of(
                        (Supplier<BusinessException>) BoardCheerClubMissingException::new,
                        "BOARD_CHEER_CLUB_MISSING",
                        ErrorType.INVALID),
                Arguments.of(
                        (Supplier<BusinessException>) BoardGameMissingException::new,
                        "BOARD_GAME_MISSING",
                        ErrorType.INVALID),
                Arguments.of(
                        (Supplier<BusinessException>) BoardWriterBlockedException::new,
                        "BOARD_WRITER_BLOCKED",
                        ErrorType.INVALID),
                Arguments.of(
                        (Supplier<BusinessException>) BoardCursorInvalidException::new,
                        "BOARD_CURSOR_INVALID",
                        ErrorType.INVALID));
    }

    @ParameterizedTest(name = "{1}")
    @MethodSource("exceptions")
    @DisplayName("예외마다 전용 코드와 타입을 가진다")
    void mapsToErrorCode(Supplier<BusinessException> factory, String code, ErrorType type) {
        // when
        BusinessException exception = factory.get();

        // then
        assertThat(exception.getErrorCode().name()).isEqualTo(code);
        assertThat(exception.getErrorCode().type()).isEqualTo(type);
        assertThat(exception.getMessage()).isNotBlank();
    }
}
