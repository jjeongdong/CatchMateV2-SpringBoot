package com.back.catchmate.board.application.dto.response;

import com.back.catchmate.club.application.dto.response.ClubResponse;
import com.back.catchmate.club.domain.model.Club;
import com.back.catchmate.game.domain.model.Game;

import java.time.LocalDateTime;

public record GameResponse(
        Long gameId,
        LocalDateTime gameStartDate,
        String location,
        ClubResponse homeClub,
        ClubResponse awayClub
) {
    public static GameResponse from(Game game, Club homeClub, Club awayClub) {
        if (game == null) {
            return null;
        }

        return new GameResponse(
                game.getId(),
                game.getGameStartDate(),
                game.getLocation(),
                homeClub != null ? ClubResponse.from(homeClub) : null,
                awayClub != null ? ClubResponse.from(awayClub) : null
        );
    }
}
