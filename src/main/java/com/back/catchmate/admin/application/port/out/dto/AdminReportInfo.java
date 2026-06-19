package com.back.catchmate.admin.application.port.out.dto;

import java.time.LocalDateTime;

public record AdminReportInfo(
        Long reportId,
        Long reporterId,
        Long reportedUserId,
        String reason,
        String description,
        LocalDateTime createdAt,
        boolean completed
) {
}
