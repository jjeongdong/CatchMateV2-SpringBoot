package com.back.catchmate.report.application.port.in;

import com.back.catchmate.report.application.dto.response.ReportInternalResponse;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

public interface ReportAdminQueryUseCase {
    ReportInternalResponse getReport(Long reportId);

    Page<ReportInternalResponse> getReportList(Pageable pageable);

    long getTotalReportCount();

    long getPendingReportCount();
}
