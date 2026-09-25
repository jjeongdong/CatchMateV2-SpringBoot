package com.back.catchmate.game.dto.response;

import java.time.LocalDateTime;

public record GameSummary(
        Long gameId, LocalDateTime gameStartDate, String location, Long homeClubId, Long awayClubId) {}
