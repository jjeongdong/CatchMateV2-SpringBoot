package com.back.catchmate.orchestration.report;

import com.back.catchmate.application.report.service.ReportService;
import com.back.catchmate.application.user.service.UserService;
import com.back.catchmate.domain.report.model.Report;
import com.back.catchmate.domain.user.model.User;
import com.back.catchmate.orchestration.report.dto.command.ReportCreateCommand;
import com.back.catchmate.orchestration.report.dto.response.ReportCreateResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

@Component
@RequiredArgsConstructor
public class ReportOrchestrator {
    private final ReportService reportService;
    private final UserService userService;

    @Transactional
    public ReportCreateResponse createReport(Long reporterId, ReportCreateCommand command) {
        User reporter = userService.getUser(reporterId);
        User reportedUser = userService.getUser(command.getReportedUserId());

        Report report = reportService.createReport(
                reporter,
                reportedUser,
                command.getReason(),
                command.getDescription()
        );

        return ReportCreateResponse.from(report);
    }
}
