package com.back.catchmate.admin.application.port.out.dto;

import java.time.LocalDateTime;

public record AdminGameInfo(
        Long gameId,
        LocalDateTime gameStartDate,
        String location,
        Long homeClubId,
        Long awayClubId
) {
}
