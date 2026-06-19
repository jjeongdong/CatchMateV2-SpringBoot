package com.back.catchmate.board.application.port.out.dto;

import java.time.LocalDateTime;

public record BoardGameInfo(
        Long gameId,
        LocalDateTime gameStartDate,
        String location,
        Long homeClubId,
        Long awayClubId
) {
    public boolean isComplete() {
        return homeClubId != null && awayClubId != null && gameStartDate != null && location != null;
    }
}
