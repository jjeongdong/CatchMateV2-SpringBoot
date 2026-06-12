package com.back.catchmate.admin.application.dto.response;

import com.back.catchmate.report.domain.model.Report;

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
    public static AdminReportDetailResponse from(Report report) {
        return new AdminReportDetailResponse(
                report.getId(),
                report.getReporter().getId(),
                report.getReporter().getNickName(),
                report.getReporter().getEmail(),
                report.getReporter().getProfileImageUrl(),
                report.getReportedUser().getId(),
                report.getReportedUser().getNickName(),
                report.getReportedUser().getEmail(),
                report.getReportedUser().getProfileImageUrl(),
                report.getReason().name(),
                report.getDescription(),
                report.getCreatedAt(),
                report.isCompleted()
        );
    }
}
