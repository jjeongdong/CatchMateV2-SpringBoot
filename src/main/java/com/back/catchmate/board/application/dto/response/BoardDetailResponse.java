package com.back.catchmate.board.application.dto.response;

import com.back.catchmate.board.domain.model.Board;
import com.back.catchmate.board.domain.model.BoardButtonStatus;
import com.back.catchmate.club.application.dto.response.ClubResponse;
import com.back.catchmate.user.application.dto.response.UserResponse;
import java.time.LocalDateTime;
import java.util.List;

public record BoardDetailResponse(
        Long boardId,
        String title,
        String content,
        int currentPerson,
        int maxPerson,
        String preferredGender,
        List<String> preferredAgeRange,
        LocalDateTime liftUpDate,
        boolean bookMarked,
        String buttonStatus,
        Long myEnrollId,
        Long chatRoomId,
        ClubResponse cheerClub,
        GameResponse game,
        UserResponse user
) {
    public static BoardDetailResponse from(Board board, boolean bookMarked, BoardButtonStatus buttonStatus, Long myEnrollId, Long chatRoomId) {
        return new BoardDetailResponse(
                board.getId(),
                board.getTitle(),
                board.getContent(),
                board.getCurrentPerson(),
                board.getMaxPerson(),
                board.getPreferredGender(),
                board.getPreferredAgeRange().asList(),
                board.getLiftUpDate(),
                bookMarked,
                buttonStatus.name(),
                myEnrollId,
                chatRoomId,
                ClubResponse.from(board.getCheerClub()),
                GameResponse.from(board.getGame()),
                UserResponse.from(board.getUser())
        );
    }
}
