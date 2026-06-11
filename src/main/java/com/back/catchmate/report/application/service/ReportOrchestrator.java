package com.back.catchmate.report.application.service;

import com.back.catchmate.report.application.service.ReportService;
import com.back.catchmate.user.application.service.UserService;
import com.back.catchmate.report.domain.model.Report;
import com.back.catchmate.user.domain.model.User;
import com.back.catchmate.report.application.dto.command.ReportCreateCommand;
import com.back.catchmate.report.application.dto.response.ReportCreateResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

@Component
@Transactional(readOnly = true)
@RequiredArgsConstructor
public class ReportOrchestrator {
    private final UserService userService;
    private final ReportService reportService;

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
