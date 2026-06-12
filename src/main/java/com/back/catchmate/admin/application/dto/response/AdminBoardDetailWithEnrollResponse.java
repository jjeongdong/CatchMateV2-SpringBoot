package com.back.catchmate.admin.application.dto.response;

import com.back.catchmate.board.domain.model.Board;
import java.time.LocalDateTime;
import java.util.List;

public record AdminBoardDetailWithEnrollResponse(
        Long boardId,
        String title,
        String content,
        String writerNickname,
        LocalDateTime gameStartDate,
        String location,
        int maxPerson,
        int currentPerson,
        boolean completed,
        LocalDateTime createdAt,
        List<AdminEnrollmentResponse> enrollments
) {
    public static AdminBoardDetailWithEnrollResponse from(Board board, List<AdminEnrollmentResponse> enrollments) {
        return new AdminBoardDetailWithEnrollResponse(
                board.getId(),
                board.getTitle(),
                board.getContent(),
                board.getUser().getNickName(),
                board.getGame().getGameStartDate(),
                board.getGame().getLocation(),
                board.getMaxPerson(),
                board.getCurrentPerson(),
                board.isCompleted(),
                board.getCreatedAt(),
                enrollments
        );
    }
}
