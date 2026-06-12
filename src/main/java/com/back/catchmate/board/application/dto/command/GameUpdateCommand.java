package com.back.catchmate.board.application.dto.command;

import java.time.LocalDateTime;

public record GameUpdateCommand(
        Long homeClubId,
        Long awayClubId,
        LocalDateTime gameStartDate,
        String location
) {
}
