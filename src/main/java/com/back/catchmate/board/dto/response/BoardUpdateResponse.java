package com.back.catchmate.board.dto.response;

import com.back.catchmate.board.entity.Board;
import java.time.LocalDateTime;

public record BoardUpdateResponse(Long boardId, LocalDateTime createdAt) {
    public static BoardUpdateResponse from(Board board) {
        return new BoardUpdateResponse(board.getId(), board.getCreatedAt());
    }
}
