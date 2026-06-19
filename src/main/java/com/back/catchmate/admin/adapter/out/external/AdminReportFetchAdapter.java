package com.back.catchmate.admin.adapter.out.external;

import com.back.catchmate.admin.application.port.out.dto.AdminReportInfo;
import com.back.catchmate.admin.application.port.out.external.ReportFetchPort;
import com.back.catchmate.report.application.dto.response.ReportInternalResponse;
import com.back.catchmate.report.application.port.in.ReportAdminQueryUseCase;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class AdminReportFetchAdapter implements ReportFetchPort {
    private final ReportAdminQueryUseCase reportAdminQueryUseCase;

    @Override
    public AdminReportInfo getReport(Long reportId) {
        return fromInternalResponse(reportAdminQueryUseCase.getReport(reportId));
    }

    @Override
    public Page<AdminReportInfo> getReportList(Pageable pageable) {
        return reportAdminQueryUseCase.getReportList(pageable).map(this::fromInternalResponse);
    }

    @Override
    public long getPendingReportCount() {
        return reportAdminQueryUseCase.getPendingReportCount();
    }

    @Override
    public long getTotalReportCount() {
        return reportAdminQueryUseCase.getTotalReportCount();
    }

    private AdminReportInfo fromInternalResponse(ReportInternalResponse response) {
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
