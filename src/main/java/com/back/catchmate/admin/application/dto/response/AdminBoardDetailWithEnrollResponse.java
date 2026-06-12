package com.back.catchmate.admin.application.dto.response;

import com.back.catchmate.board.domain.model.Board;
import com.back.catchmate.game.domain.model.Game;
import com.back.catchmate.user.domain.model.User;

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
    public static AdminBoardDetailWithEnrollResponse from(Board board, User writer, Game game, List<AdminEnrollmentResponse> enrollments) {
        return new AdminBoardDetailWithEnrollResponse(
                board.getId(),
                board.getTitle(),
                board.getContent(),
                writer != null ? writer.getNickName() : null,
                game != null ? game.getGameStartDate() : null,
                game != null ? game.getLocation() : null,
                board.getMaxPerson(),
                board.getCurrentPerson(),
                board.isCompleted(),
                board.getCreatedAt(),
                enrollments
        );
    }
}
