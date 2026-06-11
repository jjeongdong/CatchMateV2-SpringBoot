package com.back.catchmate.admin.application.port.out;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import com.back.catchmate.report.domain.model.Report;

public interface ReportFetchPort {
    long getPendingReportCount();
    Report getReport(Long reportId);
    Page<Report> getReportList(Pageable pageable);
    long getTotalReportCount();
    void updateReport(Report report);
}
