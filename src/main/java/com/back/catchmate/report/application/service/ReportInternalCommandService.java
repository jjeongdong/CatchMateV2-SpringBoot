package com.back.catchmate.report.application.service;

import com.back.catchmate.report.application.port.in.ReportInternalCommandUseCase;
import com.back.catchmate.report.application.port.out.persistence.ReportRepository;
import com.back.catchmate.report.domain.model.Report;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional
@RequiredArgsConstructor
public class ReportInternalCommandService implements ReportInternalCommandUseCase {
    private final ReportRepository reportRepository;
    private final ReportReader reportReader;

    @Override
    public void processReport(Long reportId) {
        Report report = reportReader.getReport(reportId);
        report.process();
        reportRepository.save(report);
    }
}
