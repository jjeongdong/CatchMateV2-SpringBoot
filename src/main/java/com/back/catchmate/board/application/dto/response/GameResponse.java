package com.back.catchmate.board.application.dto.response;

import com.back.catchmate.game.domain.model.Game;
import com.back.catchmate.club.application.dto.response.ClubResponse;
import java.time.LocalDateTime;

public record GameResponse(
        Long gameId,
        LocalDateTime gameStartDate,
        String location,
        ClubResponse homeClub,
        ClubResponse awayClub
) {
    public static GameResponse from(Game game) {
        if (game == null) {
            return null;
        }

        return new GameResponse(
                game.getId(),
                game.getGameStartDate(),
                game.getLocation(),
                ClubResponse.from(game.getHomeClub()),
                ClubResponse.from(game.getAwayClub())
        );
    }
}
