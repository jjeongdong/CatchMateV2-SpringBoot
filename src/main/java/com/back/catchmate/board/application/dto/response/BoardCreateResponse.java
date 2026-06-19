package com.back.catchmate.board.application.dto.response;

import com.back.catchmate.board.domain.model.Board;

import java.time.LocalDateTime;

public record BoardCreateResponse(
        Long boardId,
        LocalDateTime createdAt
) {
    public static BoardCreateResponse from(Board board) {
        return new BoardCreateResponse(board.getId(), board.getCreatedAt());
    }
}
