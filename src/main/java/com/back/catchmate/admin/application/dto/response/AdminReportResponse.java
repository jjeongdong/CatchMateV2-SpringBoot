package com.back.catchmate.admin.application.dto.response;

import com.back.catchmate.admin.application.port.out.dto.AdminReportInfo;
import com.back.catchmate.admin.application.port.out.dto.AdminUserInfo;

import java.time.LocalDateTime;

public record AdminReportResponse(
        Long reportId,
        Long reporterId,
        String reporterNickname,
        String reason,
        String description,
        LocalDateTime createdAt,
        boolean completed
) {
    public static AdminReportResponse from(AdminReportInfo report, AdminUserInfo reporter) {
        return new AdminReportResponse(
                report.reportId(),
                reporter.userId(),
                reporter.nickName(),
                report.reason(),
                report.description(),
                report.createdAt(),
                report.completed()
        );
    }
}
