package com.back.catchmate.admin.application.port.out;

import com.back.catchmate.common.page.DomainPage;
import com.back.catchmate.common.page.DomainPageable;
import com.back.catchmate.report.domain.model.Report;

public interface ReportFetchPort {
    long getPendingReportCount();
    Report getReport(Long reportId);
    DomainPage<Report> getReportList(DomainPageable pageable);
    long getTotalReportCount();
    void updateReport(Report report);
}
