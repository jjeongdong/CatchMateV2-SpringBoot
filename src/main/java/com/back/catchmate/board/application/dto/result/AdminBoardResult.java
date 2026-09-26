package com.back.catchmate.board.application.dto.result;

import com.back.catchmate.board.domain.Board;
import java.time.LocalDateTime;

public record AdminBoardResult(
        Long boardId,
        String title,
        String content,
        boolean completed,
        int currentPerson,
        int maxPerson,
        LocalDateTime createdAt) {
    public static AdminBoardResult from(Board board) {
        return new AdminBoardResult(
                board.getId(),
                board.getTitle(),
                board.getContent(),
                board.isCompleted(),
                board.getCurrentPerson(),
                board.getMaxPerson(),
                board.getCreatedAt());
    }
}
