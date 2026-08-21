package com.back.catchmate.admin.adapter.out.external;

import com.back.catchmate.admin.application.port.out.external.ReportCommandPort;
import com.back.catchmate.report.service.ReportService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class AdminReportCommandAdapter implements ReportCommandPort {
    private final ReportService reportService;

    @Override
    public void processReport(Long reportId) {
        reportService.processReport(reportId);
    }
}
