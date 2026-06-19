package com.back.catchmate.chat.application.port.out.dto;

public record ChatBoardInfo(
        Long boardId,
        String title,
        String content,
        int currentPerson,
        int maxPerson,
        Long userId,
        Long cheerClubId,
        Long gameId
) {
}
