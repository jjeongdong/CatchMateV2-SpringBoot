package com.back.catchmate.domain.report.model;

import com.back.catchmate.domain.common.permission.ResourceOwnership;
import com.back.catchmate.domain.user.model.User;
import lombok.Builder;
import lombok.Getter;
import report.enums.ReportReason;

import java.time.LocalDateTime;

@Getter
@Builder
public class Report implements ResourceOwnership {
    private Long id;
    private User reporter;      // 신고자
    private User reportedUser;  // 신고 대상자
    private ReportReason reason;
    private String description;
    private LocalDateTime createdAt;
    private boolean completed;

    public static Report createReport(User reporter, User reportedUser, ReportReason reason, String description) {
        return Report.builder()
                .reporter(reporter)
                .reportedUser(reportedUser)
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
        return reporter.getId();
    }
}
