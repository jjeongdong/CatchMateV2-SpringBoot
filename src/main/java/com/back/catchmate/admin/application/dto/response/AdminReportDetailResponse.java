package com.back.catchmate.admin.application.dto.response;

import com.back.catchmate.report.domain.model.Report;
import com.back.catchmate.user.domain.model.User;

import java.time.LocalDateTime;

public record AdminReportDetailResponse(
        Long reportId,
        Long reporterId,
        String reporterNickname,
        String reporterEmail,
        String reporterProfileImage,
        Long reportedUserId,
        String reportedUserNickname,
        String reportedUserEmail,
        String reportedUserProfileImage,
        String reason,
        String description,
        LocalDateTime createdAt,
        boolean completed
) {
    public static AdminReportDetailResponse from(Report report, User reporter, User reportedUser) {
        return new AdminReportDetailResponse(
                report.getId(),
                reporter.getId(),
                reporter.getNickName(),
                reporter.getEmail(),
                reporter.getProfileImageUrl(),
                reportedUser.getId(),
                reportedUser.getNickName(),
                reportedUser.getEmail(),
                reportedUser.getProfileImageUrl(),
                report.getReason().name(),
                report.getDescription(),
                report.getCreatedAt(),
                report.isCompleted()
        );
    }
}
