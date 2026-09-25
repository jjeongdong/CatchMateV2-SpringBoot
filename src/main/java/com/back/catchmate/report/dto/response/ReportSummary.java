package com.back.catchmate.report.dto.response;

import java.time.LocalDateTime;

public record ReportSummary(
        Long reportId,
        Long reporterId,
        Long reportedUserId,
        String reason,
        String description,
        LocalDateTime createdAt,
        boolean completed) {}
