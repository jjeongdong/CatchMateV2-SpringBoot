package com.back.catchmate.game.application.dto.response;

import java.time.LocalDateTime;

public record GameInternalResponse(
        Long gameId,
        LocalDateTime gameStartDate,
        String location,
        Long homeClubId,
        Long awayClubId
) {
}
