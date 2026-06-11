package com.back.catchmate.orchestration.admin.dto.response;

import com.back.catchmate.domain.report.model.Report;
import lombok.Builder;
import lombok.Getter;

import java.time.LocalDateTime;

@Getter
@Builder
public class AdminReportResponse {
    private Long reportId;
    private Long reporterId;
    private String reporterNickname;
    private String reason;
    private String description;
    private LocalDateTime createdAt;
    private boolean completed;

    public static AdminReportResponse from(Report report) {
        return AdminReportResponse.builder()
                .reportId(report.getId())
                .reporterId(report.getReporter().getId())
                .reporterNickname(report.getReporter().getNickName())
                .reason(report.getReason().name())
                .description(report.getDescription())
                .createdAt(report.getCreatedAt())
                .completed(report.isCompleted())
                .build();
    }
}
