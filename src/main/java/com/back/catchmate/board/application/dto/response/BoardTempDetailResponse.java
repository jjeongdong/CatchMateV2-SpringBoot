package com.back.catchmate.board.application.dto.response;

import com.back.catchmate.board.domain.model.Board;
import com.back.catchmate.club.application.dto.response.ClubResponse;
import com.back.catchmate.user.application.dto.response.UserResponse;
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
    public static BoardTempDetailResponse from(Board board) {
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
                board.getCheerClub() != null ? ClubResponse.from(board.getCheerClub()) : null,
                board.getGame() != null ? GameResponse.from(board.getGame()) : null,
                board.getUser() != null ? UserResponse.from(board.getUser()) : null
        );
    }
}
