package com.back.catchmate.admin.application.dto.response;

import com.back.catchmate.admin.application.port.out.dto.AdminBoardInfo;
import com.back.catchmate.admin.application.port.out.dto.AdminGameInfo;
import com.back.catchmate.admin.application.port.out.dto.AdminUserInfo;

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
        List<AdminEnrollmentDetailResponse> enrollments
) {
    public static AdminBoardDetailResponse from(AdminBoardInfo board, AdminUserInfo writer, AdminGameInfo game, List<AdminEnrollmentDetailResponse> enrollments) {
        return new AdminBoardDetailResponse(
                board.boardId(),
                board.title(),
                board.content(),
                writer != null ? writer.nickName() : null,
                game != null ? game.gameStartDate() : null,
                game != null ? game.location() : null,
                board.maxPerson(),
                board.currentPerson(),
                board.completed(),
                board.createdAt(),
                enrollments
        );
    }
}
