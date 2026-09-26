package com.back.catchmate.board.application.dto.result;

import com.back.catchmate.board.domain.Board;
import java.time.LocalDateTime;

public record BoardCreateResult(Long boardId, LocalDateTime createdAt) {
    public static BoardCreateResult from(Board board) {
        return new BoardCreateResult(board.getId(), board.getCreatedAt());
    }
}
