package com.back.catchmate.report.domain.model;

import com.back.catchmate.common.error.ErrorCode;
import com.back.catchmate.common.error.exception.BaseException;
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
public class Report {
    private Long id;
    private Long reporterId;
    private Long reportedUserId;
    private ReportReason reason;
    private String description;
    private LocalDateTime createdAt;
    private boolean completed;

    public static Report createReport(Long reporterId, Long reportedUserId, ReportReason reason, String description) {
        if (reporterId.equals(reportedUserId)) {
            throw new BaseException(ErrorCode.CANNOT_REPORT_SELF);
        }

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
}
