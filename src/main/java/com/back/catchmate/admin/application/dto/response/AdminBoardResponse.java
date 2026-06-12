package com.back.catchmate.admin.application.dto.response;

import com.back.catchmate.board.domain.model.Board;
import java.time.LocalDateTime;

public record AdminBoardResponse(
        Long boardId,
        String title,
        String content,
        boolean completed,
        int currentPerson,
        int maxPerson,
        LocalDateTime createdAt
) {
    public static AdminBoardResponse from(Board board) {
        return new AdminBoardResponse(
                board.getId(),
                board.getTitle(),
                board.getContent(),
                board.isCompleted(),
                board.getCurrentPerson(),
                board.getMaxPerson(),
                board.getCreatedAt()
        );
    }
}
