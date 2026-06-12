package com.back.catchmate.report.domain.model;

import com.back.catchmate.global.authorization.common.ResourceOwnership;
import com.back.catchmate.report.domain.enums.ReportReason;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Getter
@Builder
@AllArgsConstructor(access = AccessLevel.PRIVATE)
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class Report implements ResourceOwnership {
    private Long id;
    private Long reporterId;
    private Long reportedUserId;
    private ReportReason reason;
    private String description;
    private LocalDateTime createdAt;
    private boolean completed;

    public static Report createReport(Long reporterId, Long reportedUserId, ReportReason reason, String description) {
        return Report.builder()
                .reporterId(reporterId)
                .reportedUserId(reportedUserId)
                .reason(reason)
                .description(description)
                .createdAt(LocalDateTime.now())
                .completed(false)
                .build();
    }

    public void process() {
        this.completed = true;
    }

    @Override
    public Long getOwnershipId() {
        return reporterId;
    }
}
