package com.back.catchmate.report.application.dto.command;

import com.back.catchmate.report.domain.model.ReportReason;

public record ReportCreateCommand(
        Long reportedUserId,
        ReportReason reason,
        String description
) {}
