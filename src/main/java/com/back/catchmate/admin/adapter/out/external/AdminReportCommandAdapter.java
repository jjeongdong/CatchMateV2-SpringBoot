package com.back.catchmate.admin.adapter.out.external;

import com.back.catchmate.admin.application.port.out.external.ReportCommandPort;
import com.back.catchmate.report.application.port.in.ReportInternalCommandUseCase;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class AdminReportCommandAdapter implements ReportCommandPort {
    private final ReportInternalCommandUseCase reportInternalCommandUseCase;

    @Override
    public void processReport(Long reportId) {
        reportInternalCommandUseCase.processReport(reportId);
    }
}
