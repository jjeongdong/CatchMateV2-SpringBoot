package com.back.catchmate.admin.application.dto.response;

import com.back.catchmate.admin.application.port.out.dto.AdminReportInfo;
import com.back.catchmate.admin.application.port.out.dto.AdminUserInfo;

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
    public static AdminReportDetailResponse from(AdminReportInfo report, AdminUserInfo reporter, AdminUserInfo reportedUser) {
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
                report.completed()
        );
    }
}
