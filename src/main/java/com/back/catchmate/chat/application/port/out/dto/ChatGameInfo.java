package com.back.catchmate.chat.application.port.out.dto;

import java.time.LocalDateTime;

public record ChatGameInfo(
        Long gameId,
        LocalDateTime gameStartDate,
        Long homeClubId,
        Long awayClubId,
        String location
) {
}
