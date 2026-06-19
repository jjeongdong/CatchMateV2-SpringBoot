package com.back.catchmate.admin.application.port.out.dto;

import java.time.LocalDateTime;

public record AdminBoardInfo(
        Long boardId,
        String title,
        String content,
        int maxPerson,
        int currentPerson,
        Long userId,
        Long gameId,
        boolean completed,
        LocalDateTime createdAt
) {
}
