package com.back.catchmate.report.service;

import com.back.catchmate.common.error.ErrorCode;
import com.back.catchmate.common.error.exception.BaseException;
import com.back.catchmate.report.dto.request.ReportCreateRequest;
import com.back.catchmate.report.dto.response.ReportCreateResponse;
import com.back.catchmate.report.dto.response.ReportSummary;
import com.back.catchmate.report.entity.Report;
import com.back.catchmate.report.repository.ReportRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class ReportService {
    private final ReportRepository reportRepository;

    @Transactional
    public ReportCreateResponse createReport(Long reporterId, ReportCreateRequest request) {
        Report report =
                Report.createReport(reporterId, request.reportedUserId(), request.reason(), request.description());

        Report saved = reportRepository.save(report);

        return new ReportCreateResponse(saved.getId(), saved.getCreatedAt());
    }

    @Transactional
    public void processReport(Long reportId) {
        Report report = getReportOrThrow(reportId);
        report.process();
        reportRepository.save(report);
    }

    public ReportSummary getReportSummary(Long reportId) {
        return toSummary(getReportOrThrow(reportId));
    }

    public Page<ReportSummary> getReportSummaries(Pageable pageable) {
        PageRequest sortedPageRequest = PageRequest.of(
                pageable.getPageNumber(), pageable.getPageSize(), Sort.by(Sort.Direction.DESC, "createdAt"));

        return reportRepository.findAll(sortedPageRequest).map(this::toSummary);
    }

    public long getTotalReportCount() {
        return reportRepository.count();
    }

    public long getPendingReportCount() {
        return reportRepository.countByCompleted(false);
    }

    private Report getReportOrThrow(Long reportId) {
        return reportRepository.findById(reportId).orElseThrow(() -> new BaseException(ErrorCode.REPORT_NOT_FOUND));
    }

    private ReportSummary toSummary(Report report) {
        return new ReportSummary(
                report.getId(),
                report.getReporterId(),
                report.getReportedUserId(),
                report.getReason().name(),
                report.getDescription(),
                report.getCreatedAt(),
                report.isCompleted());
    }
}
