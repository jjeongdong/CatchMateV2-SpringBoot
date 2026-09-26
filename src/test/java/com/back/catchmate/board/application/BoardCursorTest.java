package com.back.catchmate.board.application;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.back.catchmate.board.domain.exception.BoardCursorInvalidException;
import java.nio.charset.StandardCharsets;
import java.time.LocalDateTime;
import java.util.Base64;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

class BoardCursorTest {

    @Test
    @DisplayName("인코딩한 커서를 디코딩하면 같은 정렬 키가 나온다")
    void roundTrip() {
        // given
        BoardCursor cursor = new BoardCursor(LocalDateTime.of(2026, 9, 1, 12, 0, 0, 123_456_000), 42L);

        // when
        BoardCursor decoded = BoardCursor.decode(cursor.encode());

        // then
        assertThat(decoded).isEqualTo(cursor);
        assertThat(cursor.encode()).doesNotContain("=", "+", "/");
    }

    @ParameterizedTest
    @ValueSource(strings = {"!!!", "", "2026-09-01T12:00|x", "no-delimiter", "not-a-date|1"})
    @DisplayName("손상된 커서는 BoardCursorInvalidException")
    void rejectsBrokenCursor(String raw) {
        // given (base64 가 아닌 값은 그대로, 나머지는 base64 로 감싸 내용만 망가뜨린다)
        String cursor = raw.equals("!!!")
                ? raw
                : Base64.getUrlEncoder().withoutPadding().encodeToString(raw.getBytes(StandardCharsets.UTF_8));

        // when & then
        assertThatThrownBy(() -> BoardCursor.decode(cursor)).isInstanceOf(BoardCursorInvalidException.class);
    }
}
