package com.back.catchmate.orchestration.report.dto.command;

import com.back.catchmate.domain.report.model.Report;
import com.back.catchmate.domain.user.model.User;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import com.back.catchmate.report.enums.ReportReason;

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
