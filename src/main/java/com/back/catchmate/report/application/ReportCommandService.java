package com.back.catchmate.report.application;

import com.back.catchmate.report.application.dto.command.ReportCreateCommand;
import com.back.catchmate.report.application.dto.result.ReportCreateResult;
import com.back.catchmate.report.application.dto.result.ReportProcessResult;
import com.back.catchmate.report.domain.Report;
import com.back.catchmate.report.domain.ReportRepository;
import com.back.catchmate.report.domain.event.ReportProcessedEvent;
import lombok.RequiredArgsConstructor;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class ReportCommandService {
    private final ReportRepository reportRepository;
    private final ApplicationEventPublisher eventPublisher;

    @Transactional
    public ReportCreateResult createReport(Long reporterId, ReportCreateCommand command) {
        Report report = reportRepository.save(
                Report.create(reporterId, command.reportedUserId(), command.reason(), command.description()));
        return ReportCreateResult.from(report);
    }

    @Transactional
    public ReportProcessResult processReport(Long reportId) {
        Report report = reportRepository.getById(reportId);
        report.process();
        eventPublisher.publishEvent(new ReportProcessedEvent(report.getId(), report.getReportedUserId()));
        return ReportProcessResult.from(report);
    }
}
