package com.back.catchmate.board.application.port.out.dto;

import java.time.LocalDateTime;

public record BoardGameUpsertCommand(
        Long homeClubId,
        Long awayClubId,
        LocalDateTime gameStartDate,
        String location
) {
}
