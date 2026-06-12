package com.back.catchmate.board.application.dto.response;

import com.back.catchmate.board.domain.model.Board;
import com.back.catchmate.club.application.dto.response.ClubResponse;
import com.back.catchmate.club.domain.model.Club;
import com.back.catchmate.game.domain.model.Game;
import com.back.catchmate.user.application.dto.response.UserResponse;
import com.back.catchmate.user.domain.model.User;

import java.util.List;

public record BoardTempDetailResponse(
        Long boardId,
        String title,
        String content,
        int maxPerson,
        String preferredGender,
        List<String> preferredAgeRange,
        ClubResponse cheerClub,
        GameResponse game,
        UserResponse user
) {
    public static BoardTempDetailResponse from(Board board, User user, Club cheerClub, Game game, Club homeClub, Club awayClub) {
        if (board == null) {
            return null;
        }

        return new BoardTempDetailResponse(
                board.getId(),
                board.getTitle(),
                board.getContent(),
                board.getMaxPerson(),
                board.getPreferredGender(),
                board.getPreferredAgeRange().asList(),
                cheerClub != null ? ClubResponse.from(cheerClub) : null,
                GameResponse.from(game, homeClub, awayClub),
                user != null ? UserResponse.from(user) : null
        );
    }
}
