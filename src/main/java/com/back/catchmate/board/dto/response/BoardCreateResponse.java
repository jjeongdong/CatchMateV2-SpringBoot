package com.back.catchmate.board.dto.response;

import com.back.catchmate.board.entity.Board;
import java.time.LocalDateTime;

public record BoardCreateResponse(Long boardId, LocalDateTime createdAt) {
    public static BoardCreateResponse from(Board board) {
        return new BoardCreateResponse(board.getId(), board.getCreatedAt());
    }
}
