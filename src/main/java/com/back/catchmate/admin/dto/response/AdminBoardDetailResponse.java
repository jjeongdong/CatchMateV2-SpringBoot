package com.back.catchmate.admin.dto.response;

import com.back.catchmate.board.dto.response.BoardSummary;
import com.back.catchmate.game.application.dto.api.GameInfo;
import com.back.catchmate.user.application.dto.api.UserInfo;
import java.time.LocalDateTime;
import java.util.List;

public record AdminBoardDetailResponse(
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
        List<AdminEnrollmentDetailResponse> enrollments) {
    public static AdminBoardDetailResponse from(
            BoardSummary board, UserInfo writer, GameInfo game, List<AdminEnrollmentDetailResponse> enrollments) {
        return new AdminBoardDetailResponse(
                board.boardId(),
                board.title(),
                board.content(),
                writer != null ? writer.nickName() : null,
                game != null ? game.gameStartDate() : null,
                game != null ? game.location() : null,
                board.maxPerson() != null ? board.maxPerson() : 0,
                board.currentPerson(),
                board.completed(),
                board.createdAt(),
                enrollments);
    }
}
