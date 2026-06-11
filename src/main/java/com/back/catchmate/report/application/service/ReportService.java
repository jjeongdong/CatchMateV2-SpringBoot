package com.back.catchmate.report.application.service;

import com.back.catchmate.report.application.port.out.UserFetchPort;

import com.back.catchmate.common.error.ErrorCode;
import com.back.catchmate.common.error.exception.BaseException;
import com.back.catchmate.common.page.DomainPage;
import com.back.catchmate.common.page.DomainPageable;
import com.back.catchmate.report.application.dto.command.ReportCreateCommand;
import com.back.catchmate.report.application.dto.response.ReportCreateResponse;
import com.back.catchmate.report.application.port.in.ReportUseCase;
import com.back.catchmate.report.application.port.out.ReportRepository;
import com.back.catchmate.report.domain.enums.ReportReason;
import com.back.catchmate.report.domain.model.Report;
import com.back.catchmate.user.domain.model.User;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Component
@Transactional(readOnly = true)
@RequiredArgsConstructor
public class ReportService implements ReportUseCase {

    private final ReportRepository reportRepository;

    private final UserFetchPort userFetchPort;

    @Transactional
    public ReportCreateResponse createReport(Long reporterId, ReportCreateCommand command) {
        User reporter = userFetchPort.getUser(reporterId);
        User reportedUser = userFetchPort.getUser(command.getReportedUserId());

        Report report = createReport(
                reporter,
                reportedUser,
                command.getReason(),
                command.getDescription()
        );

        return ReportCreateResponse.from(report);
    }

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

    public long getPendingReportCount() {
        return reportRepository.countByCompleted(false);
    }

    public void updateReport(Report report) {
        reportRepository.save(report);
    }
}
