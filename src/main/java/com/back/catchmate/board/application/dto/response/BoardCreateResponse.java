package com.back.catchmate.board.application.dto.response;

import java.time.LocalDateTime;

public record BoardCreateResponse(
        Long boardId,
        LocalDateTime createdAt
) {
    public static BoardCreateResponse of(Long boardId) {
        return new BoardCreateResponse(
                boardId,
                LocalDateTime.now()
        );
    }
}
