package com.back.catchmate.enroll.application.port.out.dto;

public record EnrollBoardInfo(
        Long boardId,
        Long userId,
        String title,
        String content,
        int currentPerson,
        int maxPerson,
        Long cheerClubId,
        Long gameId
) {
}
