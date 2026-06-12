package com.back.catchmate.board.application.dto.response;

import com.back.catchmate.board.domain.model.Board;
import com.back.catchmate.club.application.dto.response.ClubResponse;
import com.back.catchmate.user.application.dto.response.UserResponse;

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
    public static BoardResponse from(Board board, boolean bookMarked) {
        return new BoardResponse(
                board.getId(),
                board.getTitle(),
                board.getContent(),
                board.getCurrentPerson(),
                board.getMaxPerson(),
                bookMarked,
                ClubResponse.from(board.getCheerClub()),
                GameResponse.from(board.getGame()),
                UserResponse.from(board.getUser())
        );
    }
}
