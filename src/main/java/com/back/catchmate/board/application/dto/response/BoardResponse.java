package com.back.catchmate.board.application.dto.response;

import com.back.catchmate.board.domain.model.Board;
import com.back.catchmate.club.application.dto.response.ClubResponse;
import com.back.catchmate.club.domain.model.Club;
import com.back.catchmate.game.domain.model.Game;
import com.back.catchmate.user.application.dto.response.UserResponse;
import com.back.catchmate.user.domain.model.User;

public record BoardResponse(
        Long boardId,
        String title,
        String content,
        int currentPerson,
        int maxPerson,
        boolean bookMarked,
        ClubResponse cheerClub,
        GameResponse gameResponse,
        UserResponse userResponse
) {
    public static BoardResponse from(Board board, boolean bookMarked, User user, Club cheerClub, Game game, Club homeClub, Club awayClub) {
        return new BoardResponse(
                board.getId(),
                board.getTitle(),
                board.getContent(),
                board.getCurrentPerson(),
                board.getMaxPerson(),
                bookMarked,
                cheerClub != null ? ClubResponse.from(cheerClub) : null,
                GameResponse.from(game, homeClub, awayClub),
                user != null ? UserResponse.from(user) : null
        );
    }
}
