package com.back.catchmate.admin.dto.response;

import com.back.catchmate.report.dto.response.ReportSummary;
import com.back.catchmate.user.application.dto.api.UserInfo;
import java.time.LocalDateTime;

public record AdminReportResponse(
        Long reportId,
        Long reporterId,
        String reporterNickname,
        String reason,
        String description,
        LocalDateTime createdAt,
        boolean completed) {
    public static AdminReportResponse from(ReportSummary report, UserInfo reporter) {
        return new AdminReportResponse(
                report.reportId(),
                reporter.userId(),
                reporter.nickName(),
                report.reason(),
                report.description(),
                report.createdAt(),
                report.completed());
    }
}
