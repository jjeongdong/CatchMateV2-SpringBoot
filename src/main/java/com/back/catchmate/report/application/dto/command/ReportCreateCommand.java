package com.back.catchmate.report.application.dto.command;

import com.back.catchmate.report.domain.enums.ReportReason;

public record ReportCreateCommand(
        Long reportedUserId,
        ReportReason reason,
        String description
) {}
