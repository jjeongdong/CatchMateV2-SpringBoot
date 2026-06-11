package com.back.catchmate.admin.adapter.out.external;

import com.back.catchmate.admin.application.port.out.ReportFetchPort;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import com.back.catchmate.report.application.service.ReportService;
import com.back.catchmate.report.domain.model.Report;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class AdminReportFetchAdapter implements ReportFetchPort {
    private final ReportService reportService;

    @Override
    public long getPendingReportCount() {
        return reportService.getPendingReportCount();
    }

    @Override
    public Report getReport(Long reportId) {
        return reportService.getReport(reportId);
    }

    @Override
    public Page<Report> getReportList(Pageable pageable) {
        return reportService.getReportList(pageable);
    }

    @Override
    public long getTotalReportCount() {
        return reportService.getTotalReportCount();
    }

    @Override
    public void updateReport(Report report) {
        reportService.updateReport(report);
    }
}
