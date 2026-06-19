package com.back.catchmate.enroll.application.port.out.dto;

import java.time.LocalDateTime;

public record EnrollGameInfo(
        Long gameId,
        LocalDateTime gameStartDate,
        Long homeClubId,
        Long awayClubId,
        String location
) {
}
