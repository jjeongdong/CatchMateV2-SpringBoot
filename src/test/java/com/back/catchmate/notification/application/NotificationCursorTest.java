package com.back.catchmate.notification.application;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.back.catchmate.notification.domain.exception.NotificationCursorInvalidException;
import java.nio.charset.StandardCharsets;
import java.time.LocalDateTime;
import java.util.Base64;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

class NotificationCursorTest {

    @Test
    @DisplayName("인코딩한 커서를 다시 풀면 같은 값이다 (마이크로초 포함)")
    void roundTrip() {
        NotificationCursor cursor = new NotificationCursor(LocalDateTime.of(2026, 9, 1, 12, 0, 0, 123_456_000), 42L);

        assertThat(NotificationCursor.decode(cursor.encode())).isEqualTo(cursor);
    }

    @ParameterizedTest
    @ValueSource(strings = {"!!!", "bm8tZGVsaW1pdGVy", "MjAyNnwx", "MjAyNi0wOS0wMVQxMjowMHxhYmM"})
    @DisplayName("손상된 커서는 NotificationCursorInvalidException")
    void rejectsBrokenCursor(String cursor) {
        assertThatThrownBy(() -> NotificationCursor.decode(cursor))
                .isInstanceOf(NotificationCursorInvalidException.class);
    }

    @Test
    @DisplayName("테스트 입력 확인: 위 커서들은 차례로 base64 아님·구분자 없음·날짜 아님·id 숫자 아님이다")
    void brokenCursorSamples() {
        assertThat(decode("bm8tZGVsaW1pdGVy")).isEqualTo("no-delimiter");
        assertThat(decode("MjAyNnwx")).isEqualTo("2026|1");
        assertThat(decode("MjAyNi0wOS0wMVQxMjowMHxhYmM")).isEqualTo("2026-09-01T12:00|abc");
    }

    private static String decode(String value) {
        return new String(Base64.getUrlDecoder().decode(value), StandardCharsets.UTF_8);
    }
}
