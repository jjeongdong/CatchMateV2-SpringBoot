package com.back.catchmate.board.application.dto.result;

import com.back.catchmate.board.domain.Board;
import com.back.catchmate.club.application.dto.api.ClubInfo;
import com.back.catchmate.enroll.application.dto.api.EnrollInfo;
import com.back.catchmate.game.application.dto.api.GameInfo;
import com.back.catchmate.user.application.dto.api.UserInfo;
import java.time.LocalDateTime;
import java.util.List;

public record AdminBoardDetailResult(
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
        List<EnrollmentView> enrollments) {
    public static AdminBoardDetailResult of(
            Board board, UserInfo writer, GameInfo game, List<EnrollmentView> enrollments) {
        return new AdminBoardDetailResult(
                board.getId(),
                board.getTitle(),
                board.getContent(),
                writer != null ? writer.nickName() : null,
                game != null ? game.gameStartDate() : null,
                game != null ? game.location() : null,
                board.getMaxPerson(),
                board.getCurrentPerson(),
                board.isCompleted(),
                board.getCreatedAt(),
                enrollments);
    }

    // 탈퇴한 신청자는 사용자 필드를 비운다 (옛 코드는 NPE).
    public record EnrollmentView(
            Long enrollId,
            Long userId,
            String profileImageUrl,
            String nickName,
            String clubName,
            Character gender,
            String email,
            String provider,
            String status,
            LocalDateTime requestedAt) {
        public static EnrollmentView of(EnrollInfo enroll, UserInfo user, ClubInfo club) {
            return new EnrollmentView(
                    enroll.enrollId(),
                    enroll.userId(),
                    user != null ? user.profileImageUrl() : null,
                    user != null ? user.nickName() : null,
                    club != null ? club.name() : null,
                    user != null ? user.gender() : null,
                    user != null ? user.email() : null,
                    user != null ? user.provider() : null,
                    enroll.acceptStatus(),
                    enroll.requestedAt());
        }
    }
}
