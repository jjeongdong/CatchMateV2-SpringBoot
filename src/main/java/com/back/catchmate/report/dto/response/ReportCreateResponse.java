package com.back.catchmate.report.dto.response;

import java.time.LocalDateTime;

public record ReportCreateResponse(Long reportId, LocalDateTime createdAt) {}
