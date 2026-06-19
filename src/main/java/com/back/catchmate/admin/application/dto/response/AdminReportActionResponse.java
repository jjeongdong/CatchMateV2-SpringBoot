package com.back.catchmate.admin.application.dto.response;


public record AdminReportActionResponse(
        Long reportId,
        Long reportedUserId
) {
    public static AdminReportActionResponse of(Long reportId, Long reportedUserId) {
        return new AdminReportActionResponse(reportId, reportedUserId);
    }
}
