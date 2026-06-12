package com.back.catchmate.admin.application.dto.response;


public record ReportActionResponse(
        Long reportId,
        Long reportedUserId
) {
    public static ReportActionResponse of(Long reportId, Long reportedUserId) {
        return new ReportActionResponse(reportId, reportedUserId);
    }
}
