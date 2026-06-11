package com.back.catchmate.board.application.dto.response;

import com.back.catchmate.game.domain.model.Game;
import com.back.catchmate.club.application.dto.response.ClubResponse;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;

import java.time.LocalDateTime;

@Getter
@Builder
@AllArgsConstructor
public class GameResponse {
    private Long gameId;
    private LocalDateTime gameStartDate;
    private String location;
    private ClubResponse homeClub;
    private ClubResponse awayClub;

    public static GameResponse from(Game game) {
        if (game == null) {
            return null;
        }

        return GameResponse.builder()
                .gameId(game.getId())
                .gameStartDate(game.getGameStartDate())
                .location(game.getLocation())
                .homeClub(ClubResponse.from(game.getHomeClub()))
                .awayClub(ClubResponse.from(game.getAwayClub()))
                .build();
    }
}
