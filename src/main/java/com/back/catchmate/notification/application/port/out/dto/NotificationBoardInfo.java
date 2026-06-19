package com.back.catchmate.notification.application.port.out.dto;

public record NotificationBoardInfo(
        Long boardId,
        Long gameId,
        String title
) {
}
