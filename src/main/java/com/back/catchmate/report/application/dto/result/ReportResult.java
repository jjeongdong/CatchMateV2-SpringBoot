package com.back.catchmate.report.application.dto.result;

import com.back.catchmate.report.domain.Report;
import com.back.catchmate.user.application.dto.api.UserInfo;
import java.time.LocalDateTime;

public record ReportResult(
        Long reportId,
        Long reporterId,
        String reporterNickname,
        String reason,
        String description,
        LocalDateTime createdAt,
        boolean completed) {

    // 신고자가 탈퇴해 조회되지 않으면 목록 전체를 실패시키지 않고 닉네임만 비운다.
    public static ReportResult of(Report report, UserInfo reporter) {
        return new ReportResult(
                report.getId(),
                report.getReporterId(),
                reporter != null ? reporter.nickName() : null,
                report.getReason() != null ? report.getReason().name() : null,
                report.getDescription(),
                report.getCreatedAt(),
                report.isCompleted());
    }
}
