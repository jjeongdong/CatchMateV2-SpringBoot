package com.back.catchmate.admin.adapter.out.external;

import com.back.catchmate.admin.application.port.out.dto.AdminReportInfo;
import com.back.catchmate.admin.application.port.out.external.ReportFetchPort;
import com.back.catchmate.report.dto.response.ReportSummary;
import com.back.catchmate.report.service.ReportService;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class AdminReportFetchAdapter implements ReportFetchPort {
    private final ReportService reportService;

    @Override
    public AdminReportInfo getReport(Long reportId) {
        return fromInternalResponse(reportService.getReportSummary(reportId));
    }

    @Override
    public Page<AdminReportInfo> getReportList(Pageable pageable) {
        return reportService.getReportSummaries(pageable).map(this::fromInternalResponse);
    }

    @Override
    public long getPendingReportCount() {
        return reportService.getPendingReportCount();
    }

    @Override
    public long getTotalReportCount() {
        return reportService.getTotalReportCount();
    }

    private AdminReportInfo fromInternalResponse(ReportSummary response) {
        return new AdminReportInfo(
                response.reportId(),
                response.reporterId(),
                response.reportedUserId(),
                response.reason(),
                response.description(),
                response.createdAt(),
                response.completed()
        );
    }
}
