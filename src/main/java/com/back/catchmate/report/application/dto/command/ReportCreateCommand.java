package com.back.catchmate.report.application.dto.command;

import com.back.catchmate.report.domain.model.Report;
import com.back.catchmate.user.domain.model.User;
import com.back.catchmate.report.domain.enums.ReportReason;

public record ReportCreateCommand(
        Long reportedUserId,
        ReportReason reason,
        String description
) {
    public Report toEntity(User reporter, User reportedUser) {
        return Report.builder()
                .reporter(reporter)
                .reportedUser(reportedUser)
                .reason(this.reason)
                .description(this.description)
                .build();
    }
}
