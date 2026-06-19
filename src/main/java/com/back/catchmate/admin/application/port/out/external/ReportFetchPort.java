package com.back.catchmate.admin.application.port.out.external;

import com.back.catchmate.admin.application.port.out.dto.AdminReportInfo;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

public interface ReportFetchPort {
    AdminReportInfo getReport(Long reportId);

    Page<AdminReportInfo> getReportList(Pageable pageable);

    long getPendingReportCount();

    long getTotalReportCount();
}
