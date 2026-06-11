package com.back.catchmate.report.application.port.in;

import com.back.catchmate.report.application.dto.command.ReportCreateCommand;
import com.back.catchmate.report.application.dto.response.ReportCreateResponse;

public interface ReportUseCase {
    ReportCreateResponse createReport(Long reporterId, ReportCreateCommand command);
}
