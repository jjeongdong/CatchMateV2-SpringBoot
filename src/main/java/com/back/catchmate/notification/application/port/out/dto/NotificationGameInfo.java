package com.back.catchmate.notification.application.port.out.dto;

import java.time.LocalDateTime;

public record NotificationGameInfo(
        Long gameId,
        LocalDateTime gameStartDate,
        String location,
        Long homeClubId,
        Long awayClubId
) {
}
