package com.back.catchmate.application.report.service;

import com.back.catchmate.domain.common.page.DomainPage;
import com.back.catchmate.domain.common.page.DomainPageable;
import com.back.catchmate.domain.report.model.Report;
import com.back.catchmate.domain.report.repository.ReportRepository;
import com.back.catchmate.domain.user.model.User;
import com.back.catchmate.error.ErrorCode;
import com.back.catchmate.error.exception.BaseException;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import com.back.catchmate.report.enums.ReportReason;

@Service
@RequiredArgsConstructor
public class ReportService {
    private final ReportRepository reportRepository;

    public Report createReport(User reporter, User reportedUser, ReportReason reason, String description) {
        // 1. 비즈니스 로직 검증: 본인 신고 불가
        if (reporter.getId().equals(reportedUser.getId())) {
            throw new BaseException(ErrorCode.CANNOT_REPORT_SELF);
        }

        // 2. 도메인 모델 생성
        Report report = Report.createReport(
                reporter,
                reportedUser,
                reason,
                description
        );

        // 3. 저장
        return reportRepository.save(report);
    }

    public Report getReport(Long reportId) {
        return reportRepository.findById(reportId)
                .orElseThrow(() -> new BaseException(ErrorCode.REPORT_NOT_FOUND));
    }

    public DomainPage<Report> getReportList(DomainPageable pageable) {
        return reportRepository.findAll(pageable);
    }

    public long getTotalReportCount() {
        return reportRepository.count();
    }

    public void updateReport(Report report) {
        reportRepository.save(report);
    }

}
