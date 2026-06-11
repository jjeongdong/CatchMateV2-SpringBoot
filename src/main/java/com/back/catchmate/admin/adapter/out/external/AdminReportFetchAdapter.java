package com.back.catchmate.admin.adapter.out.external;

import com.back.catchmate.admin.application.port.out.ReportFetchPort;
import com.back.catchmate.common.page.DomainPage;
import com.back.catchmate.common.page.DomainPageable;
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
    public DomainPage<Report> getReportList(DomainPageable pageable) {
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
