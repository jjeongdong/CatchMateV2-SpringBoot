package com.back.catchmate.game.application.dto.response;

import com.back.catchmate.game.application.dto.GameClubInfo;
import com.back.catchmate.game.domain.model.Game;

import java.time.LocalDateTime;

/**
 * 글 작성 시 프론트가 직관할 경기를 선택하기 위한 화면용 응답.
 */
public record GameResponse(
        Long gameId,
        LocalDateTime gameStartDate,
        String location,
        GameClubView homeClub,
        GameClubView awayClub
) {
    public static GameResponse of(Game game, GameClubInfo homeClub, GameClubInfo awayClub) {
        return new GameResponse(
                game.getId(),
                game.getGameStartDate(),
                game.getLocation(),
                GameClubView.from(homeClub),
                GameClubView.from(awayClub)
        );
    }
}
