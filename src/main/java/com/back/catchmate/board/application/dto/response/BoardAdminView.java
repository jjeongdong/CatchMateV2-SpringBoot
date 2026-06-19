package com.back.catchmate.board.application.dto.response;

import java.time.LocalDateTime;

/**
 * Admin 정문(BoardAdminQueryUseCase) 전용 published contract.
 * 도메인 모델 Board 가 호출자(admin 어댑터)로 노출되지 않도록 자체 record 로 분리.
 */
public record BoardAdminView(
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
