package com.back.catchmate.report.application.dto.result;

import com.back.catchmate.report.domain.Report;
import com.back.catchmate.user.application.dto.api.UserInfo;
import java.time.LocalDateTime;

public record ReportDetailResult(
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

    public static ReportDetailResult of(Report report, UserInfo reporter, UserInfo reportedUser) {
        return new ReportDetailResult(
                report.getId(),
                reporter.userId(),
                reporter.nickName(),
                reporter.email(),
                reporter.profileImageUrl(),
                reportedUser.userId(),
                reportedUser.nickName(),
                reportedUser.email(),
                reportedUser.profileImageUrl(),
                report.getReason() != null ? report.getReason().name() : null,
                report.getDescription(),
                report.getCreatedAt(),
                report.isCompleted());
    }
}
