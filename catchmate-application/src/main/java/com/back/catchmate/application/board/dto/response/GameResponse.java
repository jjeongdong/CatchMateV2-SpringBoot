package com.back.catchmate.application.board.dto.response;

import com.back.catchmate.application.club.dto.response.ClubResponse;
import com.back.catchmate.domain.game.model.Game;
import lombok.Builder;
import lombok.Getter;

import java.time.LocalDateTime;

@Getter
@Builder
public class GameResponse {
    private Long gameId;
    private LocalDateTime gameStartDate;
    private String location;
    private ClubResponse homeClub;
    private ClubResponse awayClub;

    public static GameResponse from(Game game) {
        return GameResponse.builder()
                .gameId(game.getId())
                .gameStartDate(game.getGameStartDate())
                .location(game.getLocation())
                .homeClub(ClubResponse.from(game.getHomeClub()))
                .awayClub(ClubResponse.from(game.getAwayClub()))
                .build();
    }
}
