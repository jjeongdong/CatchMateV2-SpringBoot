package com.back.catchmate.report.application.dto.command;

import com.back.catchmate.report.domain.model.Report;
import com.back.catchmate.user.domain.model.User;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import com.back.catchmate.report.domain.enums.ReportReason;

@Getter
@Builder
@AllArgsConstructor
public class ReportCreateCommand {
    private Long reportedUserId;
    private ReportReason reason;
    private String description;

    public Report toEntity(User reporter, User reportedUser) {
        return Report.builder()
                .reporter(reporter)
                .reportedUser(reportedUser)
                .reason(this.reason)
                .description(this.description)
                .build();
    }
}
