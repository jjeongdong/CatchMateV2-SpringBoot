package com.back.catchmate.report.application.dto.response;

import com.back.catchmate.report.domain.model.Report;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;

import java.time.LocalDateTime;

@Getter
@Builder
@AllArgsConstructor
public class ReportCreateResponse {
    private Long reportId;
    private LocalDateTime createdAt;

    public static ReportCreateResponse from(Report report) {
        return ReportCreateResponse.builder()
                .reportId(report.getId())
                .createdAt(LocalDateTime.now())
                .build();
    }
}
