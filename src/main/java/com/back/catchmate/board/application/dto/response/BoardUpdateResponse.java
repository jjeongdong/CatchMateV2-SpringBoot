package com.back.catchmate.board.application.dto.response;

import java.time.LocalDateTime;

public record BoardUpdateResponse(
        Long boardId,
        LocalDateTime createdAt
) {
    public static BoardUpdateResponse of(Long boardId) {
        return new BoardUpdateResponse(
                boardId,
                LocalDateTime.now()
        );
    }
}
