package com.back.catchmate.report.domain.model;

import com.back.catchmate.common.permission.ResourceOwnership;
import com.back.catchmate.user.domain.model.User;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import com.back.catchmate.report.domain.enums.ReportReason;

import java.time.LocalDateTime;

@Getter
@Builder
@AllArgsConstructor(access = AccessLevel.PRIVATE)
@NoArgsConstructor(access = AccessLevel.PROTECTED)
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
