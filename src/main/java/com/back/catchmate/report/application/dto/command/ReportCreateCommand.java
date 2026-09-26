package com.back.catchmate.report.application.dto.command;

import com.back.catchmate.report.domain.ReportReason;

public record ReportCreateCommand(Long reportedUserId, ReportReason reason, String description) {}
