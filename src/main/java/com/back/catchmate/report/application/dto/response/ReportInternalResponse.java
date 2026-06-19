package com.back.catchmate.report.application.dto.response;

import java.time.LocalDateTime;

public record ReportInternalResponse(
        Long reportId,
        Long reporterId,
        Long reportedUserId,
        String reason,
        String description,
        LocalDateTime createdAt,
        boolean completed
) {
}
