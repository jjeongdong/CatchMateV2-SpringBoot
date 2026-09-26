package com.back.catchmate.report.application.dto.result;

import com.back.catchmate.report.domain.Report;
import java.time.LocalDateTime;

public record ReportCreateResult(Long reportId, LocalDateTime createdAt) {
    public static ReportCreateResult from(Report report) {
        return new ReportCreateResult(report.getId(), report.getCreatedAt());
    }
}
