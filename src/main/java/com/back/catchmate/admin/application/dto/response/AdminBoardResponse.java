package com.back.catchmate.admin.application.dto.response;

import com.back.catchmate.admin.application.port.out.dto.AdminBoardInfo;

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
    public static AdminBoardResponse from(AdminBoardInfo board) {
        return new AdminBoardResponse(
                board.boardId(),
                board.title(),
                board.content(),
                board.completed(),
                board.currentPerson(),
                board.maxPerson(),
                board.createdAt()
        );
    }
}
