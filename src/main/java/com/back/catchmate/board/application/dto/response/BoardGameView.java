package com.back.catchmate.board.application.dto.response;

import java.time.LocalDateTime;

/**
 * board 응답에 임베드되는 game 요약. board 컨텍스트가 자체 소유한 API 계약.
 */
public record BoardGameView(
        Long gameId,
        LocalDateTime gameStartDate,
        String location,
        BoardClubView homeClub,
        BoardClubView awayClub
) {
}
