package com.back.catchmate.bookmark.application.port.out.dto;

public record BookmarkBoardInfo(
        Long boardId,
        Long userId,
        Long gameId,
        Long cheerClubId,
        String title,
        String content,
        int currentPerson,
        int maxPerson
) {
}
