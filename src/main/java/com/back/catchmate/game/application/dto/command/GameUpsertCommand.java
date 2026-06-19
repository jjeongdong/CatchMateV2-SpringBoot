package com.back.catchmate.game.application.dto.command;

import java.time.LocalDateTime;

public record GameUpsertCommand(
        Long homeClubId,
        Long awayClubId,
        LocalDateTime gameStartDate,
        String location
) {
}
