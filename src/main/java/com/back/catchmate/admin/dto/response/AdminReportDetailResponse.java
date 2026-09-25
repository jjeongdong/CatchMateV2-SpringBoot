package com.back.catchmate.admin.dto.response;

import com.back.catchmate.report.dto.response.ReportSummary;
import com.back.catchmate.user.dto.response.UserSummary;
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
        boolean completed) {
    public static AdminReportDetailResponse from(ReportSummary report, UserSummary reporter, UserSummary reportedUser) {
        return new AdminReportDetailResponse(
                report.reportId(),
                reporter.userId(),
                reporter.nickName(),
                reporter.email(),
                reporter.profileImageUrl(),
                reportedUser.userId(),
                reportedUser.nickName(),
                reportedUser.email(),
                reportedUser.profileImageUrl(),
                report.reason(),
                report.description(),
                report.createdAt(),
                report.completed());
    }
}
