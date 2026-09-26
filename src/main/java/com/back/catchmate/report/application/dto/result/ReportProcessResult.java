package com.back.catchmate.report.application.dto.result;

import com.back.catchmate.report.domain.Report;

public record ReportProcessResult(Long reportId, Long reportedUserId) {
    public static ReportProcessResult from(Report report) {
        return new ReportProcessResult(report.getId(), report.getReportedUserId());
    }
}
