package com.back.catchmate.report.application.service;

import com.back.catchmate.common.error.ErrorCode;
import com.back.catchmate.common.error.exception.BaseException;
import com.back.catchmate.report.application.dto.command.ReportCreateCommand;
import com.back.catchmate.report.application.dto.response.ReportCreateResponse;
import com.back.catchmate.report.application.port.in.ReportUseCase;
import com.back.catchmate.report.application.port.out.ReportRepository;
import com.back.catchmate.report.domain.enums.ReportReason;
import com.back.catchmate.report.domain.model.Report;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional(readOnly = true)
@RequiredArgsConstructor
public class ReportService implements ReportUseCase {

    private final ReportRepository reportRepository;

    @Transactional
    public ReportCreateResponse createReport(Long reporterId, ReportCreateCommand command) {
        Report report = createReport(reporterId, command.reportedUserId(), command.reason(), command.description());
        return ReportCreateResponse.from(report);
    }

    public Report createReport(Long reporterId, Long reportedUserId, ReportReason reason, String description) {
        if (reporterId.equals(reportedUserId)) {
            throw new BaseException(ErrorCode.CANNOT_REPORT_SELF);
        }
        return reportRepository.save(Report.createReport(reporterId, reportedUserId, reason, description));
    }

    public Report getReport(Long reportId) {
        return reportRepository.findById(reportId)
                .orElseThrow(() -> new BaseException(ErrorCode.REPORT_NOT_FOUND));
    }

    public Page<Report> getReportList(Pageable pageable) {
        return reportRepository.findAll(pageable);
    }

    public long getTotalReportCount() {
        return reportRepository.count();
    }

    public long getPendingReportCount() {
        return reportRepository.countByCompleted(false);
    }

    public void updateReport(Report report) {
        reportRepository.save(report);
    }
}
