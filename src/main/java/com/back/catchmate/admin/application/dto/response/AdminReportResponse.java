package com.back.catchmate.admin.application.dto.response;

import com.back.catchmate.report.domain.model.Report;
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
    public static AdminReportResponse from(Report report) {
        return new AdminReportResponse(
                report.getId(),
                report.getReporter().getId(),
                report.getReporter().getNickName(),
                report.getReason().name(),
                report.getDescription(),
                report.getCreatedAt(),
                report.isCompleted()
        );
    }
}
