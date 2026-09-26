package com.back.catchmate.game.application.dto.api;

import com.back.catchmate.game.domain.Game;
import java.time.LocalDateTime;

public record GameInfo(Long gameId, LocalDateTime gameStartDate, String location, Long homeClubId, Long awayClubId) {
    public static GameInfo from(Game game) {
        return new GameInfo(
                game.getId(), game.getGameStartDate(), game.getLocation(), game.getHomeClubId(), game.getAwayClubId());
    }
}
