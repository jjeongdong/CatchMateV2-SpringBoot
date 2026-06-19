package com.back.catchmate.report.application.service;

import com.back.catchmate.common.error.ErrorCode;
import com.back.catchmate.common.error.exception.BaseException;
import com.back.catchmate.report.application.port.out.persistence.ReportRepository;
import com.back.catchmate.report.domain.model.Report;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class ReportReader {
    private final ReportRepository reportRepository;

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
}
