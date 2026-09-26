package com.back.catchmate.board.domain;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

class BoardButtonStatusTest {

    @ParameterizedTest(name = "조회자 {0}, 신청 상태 {1} → {2}")
    @CsvSource(
            value = {
                "1, NULL, VIEW_CHAT",
                "2, NULL, APPLY",
                "2, PENDING, CANCEL",
                "2, REJECTED, REJECTED",
                "2, ACCEPTED, VIEW_CHAT"
            },
            nullValues = "NULL")
    @DisplayName("작성자(1)·신청 상태별 버튼은 옛 매핑과 같다")
    void resolve(Long requesterId, String enrollStatus, BoardButtonStatus expected) {
        assertThat(BoardButtonStatus.resolve(requesterId, 1L, enrollStatus)).isEqualTo(expected);
    }
}
