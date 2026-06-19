package com.back.catchmate.report.application.service;

import com.back.catchmate.report.application.dto.response.ReportInternalResponse;
import com.back.catchmate.report.application.port.in.ReportAdminQueryUseCase;
import com.back.catchmate.report.domain.model.Report;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional(readOnly = true)
@RequiredArgsConstructor
public class ReportInternalQueryService implements ReportAdminQueryUseCase {
    private final ReportReader reportReader;

    @Override
    public ReportInternalResponse getReport(Long reportId) {
        return toInternalResponse(reportReader.getReport(reportId));
    }

    @Override
    public Page<ReportInternalResponse> getReportList(Pageable pageable) {
        return reportReader.getReportList(pageable).map(this::toInternalResponse);
    }

    @Override
    public long getTotalReportCount() {
        return reportReader.getTotalReportCount();
    }

    @Override
    public long getPendingReportCount() {
        return reportReader.getPendingReportCount();
    }

    private ReportInternalResponse toInternalResponse(Report report) {
        return new ReportInternalResponse(
                report.getId(),
                report.getReporterId(),
                report.getReportedUserId(),
                report.getReason().name(),
                report.getDescription(),
                report.getCreatedAt(),
                report.isCompleted()
        );
    }
}
