package com.back.catchmate.report.application.dto.response;

import com.back.catchmate.report.domain.model.Report;
import java.time.LocalDateTime;

public record ReportCreateResponse(
        Long reportId,
        LocalDateTime createdAt
) {
    public static ReportCreateResponse from(Report report) {
        return new ReportCreateResponse(
                report.getId(),
                LocalDateTime.now()
        );
    }
}
