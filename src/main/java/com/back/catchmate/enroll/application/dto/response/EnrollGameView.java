package com.back.catchmate.enroll.application.dto.response;

import java.time.LocalDateTime;

/**
 * enroll 응답에 임베드되는 game 요약. enroll 컨텍스트가 자체 소유한 API 계약.
 */
public record EnrollGameView(
        Long gameId,
        LocalDateTime gameStartDate,
        String location,
        EnrollClubView homeClub,
        EnrollClubView awayClub
) {
}
