package com.back.catchmate.domain.report.service;

import com.back.catchmate.domain.common.page.DomainPage;
import com.back.catchmate.domain.common.page.DomainPageable;
import com.back.catchmate.domain.report.model.Report;
import com.back.catchmate.domain.report.repository.ReportRepository;
import error.ErrorCode;
import error.exception.BaseException;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class ReportService {
    private final ReportRepository reportRepository;

    public Report createReport(Report report) {
        return reportRepository.save(report);
    }

    public Report getReport(Long id) {
        return reportRepository.findById(id)
                .orElseThrow(() -> new BaseException(ErrorCode.REPORT_NOT_FOUND));
    }

    public DomainPage<Report> getReportList(DomainPageable pageable) {
        return reportRepository.findAll(pageable);
    }

    public long getTotalReportCount() {
        return reportRepository.count();
    }

    public void updateReport(Report report) {
        reportRepository.save(report);
    }
}
