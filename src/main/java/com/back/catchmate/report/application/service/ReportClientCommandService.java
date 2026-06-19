package com.back.catchmate.report.application.service;

import com.back.catchmate.report.application.dto.command.ReportCreateCommand;
import com.back.catchmate.report.application.dto.response.ReportCreateResponse;
import com.back.catchmate.report.application.port.in.ReportClientCommandUseCase;
import com.back.catchmate.report.application.port.out.persistence.ReportRepository;
import com.back.catchmate.report.domain.model.Report;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional
@RequiredArgsConstructor
public class ReportClientCommandService implements ReportClientCommandUseCase {
    private final ReportRepository reportRepository;

    @Override
    public ReportCreateResponse createReport(Long reporterId, ReportCreateCommand command) {
        Report report = Report.createReport(
                reporterId,
                command.reportedUserId(),
                command.reason(),
                command.description()
        );

        Report saved = reportRepository.save(report);

        return new ReportCreateResponse(
                saved.getId(),
                saved.getCreatedAt()
        );
    }
}
