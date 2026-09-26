package com.back.catchmate.board.application.dto.result;

import com.back.catchmate.board.domain.Board;
import java.time.LocalDateTime;

public record BoardUpdateResult(Long boardId, LocalDateTime createdAt) {
    public static BoardUpdateResult from(Board board) {
        return new BoardUpdateResult(board.getId(), board.getCreatedAt());
    }
}
