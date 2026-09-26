package com.back.catchmate.report.domain.event;

public record ReportProcessedEvent(Long reportId, Long reportedUserId) {}
