package com.back.catchmate.report.application;

import com.back.catchmate.global.response.OffsetPageResult;
import com.back.catchmate.report.application.dto.result.ReportDetailResult;
import com.back.catchmate.report.application.dto.result.ReportResult;
import com.back.catchmate.report.domain.Report;
import com.back.catchmate.report.domain.ReportRepository;
import com.back.catchmate.user.application.UserQueryApi;
import com.back.catchmate.user.application.dto.api.UserInfo;
import java.util.List;
import java.util.Map;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class ReportQueryService {
    private final ReportRepository reportRepository;
    private final UserQueryApi userQueryApi;

    @Transactional(readOnly = true)
    public ReportDetailResult getReport(Long reportId) {
        Report report = reportRepository.getById(reportId);
        return ReportDetailResult.of(
                report, userQueryApi.getInfo(report.getReporterId()), userQueryApi.getInfo(report.getReportedUserId()));
    }

    @Transactional(readOnly = true)
    public OffsetPageResult<ReportResult> getReports(int page, int size) {
        List<Report> reports = reportRepository.findAllLatest((long) page * size, size);
        long totalElements = reportRepository.count();
        Map<Long, UserInfo> reporterById = reports.isEmpty()
                ? Map.of()
                : userQueryApi.getInfos(
                        reports.stream().map(Report::getReporterId).distinct().toList());
        List<ReportResult> content = reports.stream()
                .map(report -> ReportResult.of(report, reporterById.get(report.getReporterId())))
                .toList();
        return OffsetPageResult.of(content, page, size, totalElements);
    }
}
