package com.back.catchmate.board.application.dto.response;

import java.time.LocalDateTime;
import java.util.List;

public record BoardInternalResponse(
        Long boardId,
        String title,
        String content,
        Integer maxPerson,
        int currentPerson,
        Long userId,
        Long cheerClubId,
        Long gameId,
        String preferredGender,
        List<String> preferredAgeRange,
        boolean completed,
        LocalDateTime createdAt,
        LocalDateTime liftUpDate
) {
}
