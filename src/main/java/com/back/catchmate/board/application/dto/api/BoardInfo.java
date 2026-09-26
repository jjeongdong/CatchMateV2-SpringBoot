package com.back.catchmate.board.application.dto.api;

import com.back.catchmate.board.domain.Board;
import java.time.LocalDateTime;
import java.util.List;

// 타 BC 계약. 필드명은 옛 BoardSummary 와 같다 (userId = 작성자).
public record BoardInfo(
        Long boardId,
        String title,
        String content,
        int maxPerson,
        int currentPerson,
        Long userId,
        Long cheerClubId,
        Long gameId,
        String preferredGender,
        List<String> preferredAgeRange,
        boolean completed,
        LocalDateTime createdAt,
        LocalDateTime liftUpDate) {
    public static BoardInfo from(Board board) {
        return new BoardInfo(
                board.getId(),
                board.getTitle(),
                board.getContent(),
                board.getMaxPerson(),
                board.getCurrentPerson(),
                board.getUserId(),
                board.getCheerClubId(),
                board.getGameId(),
                board.getPreferredGender(),
                board.getPreferredAgeRange().asList(),
                board.isCompleted(),
                board.getCreatedAt(),
                board.getLiftUpDate());
    }
}
